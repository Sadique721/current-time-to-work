package com.xcess.ocs.summaryengine.cron;

import com.xcess.ocs.constants.enums.RequestStatus;
import com.xcess.ocs.dto.RequestParameters;
import com.xcess.ocs.entity.CdrQueryConfig;

import com.xcess.ocs.entity.ReRateRequest;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.entity.VoiceRatedCdr;
import com.xcess.ocs.entity.SmsRatedCdr;
import com.xcess.ocs.entity.UsageRatedCdr;
import com.xcess.ocs.ratingengine.service.CdrRatingIntegrationService;
import com.xcess.ocs.repository.ReRateRequestRepository;
import com.xcess.ocs.repository.SmsRatedCdrRepository;
import com.xcess.ocs.repository.UsageRatedCdrRepository;
import com.xcess.ocs.repository.VoiceRatedCdrRepository;
import com.xcess.ocs.service.ReRateRequestService;
import com.xcess.ocs.utils.RegistryUtils;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class ReRatePollingScheduler {

    private final Logger log = LoggerFactory.getLogger(ReRatePollingScheduler.class);
    private final ThreadPoolExecutor workerPool;
    private final ScheduledExecutorService scheduler;

    private final AtomicLong currentPollIntervalMs;
    private final AtomicInteger consecutiveEmptyPolls = new AtomicInteger(0);
    private final AtomicInteger attemptsAtMaxInterval = new AtomicInteger(0);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicInteger maxRetryForReRateReqProcessing = new AtomicInteger(0);

    private static final int MAX_ATTEMPTS_AT_MAX_INTERVAL = 10;
    private static final int MAX_THREAD_THRESHOLD = 2;
    private static final int PAGE_SIZE = 1000;
    private static final int MAX_CONSECUTIVE_FAILURES = 5;

    @Autowired
    private ReRateRequestRepository reRateRequestRepository;

    @Autowired
    private ReRateRequestService reRateRequestService;

    @Getter
    private final CronExecutorProperties properties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VoiceRatedCdrRepository voiceRatedCdrRepository;

    @Autowired
    private SmsRatedCdrRepository smsRatedCdrRepository;

    @Autowired
    private UsageRatedCdrRepository usageRatedCdrRepository;

    @Autowired
    private CdrRatingIntegrationService ratingIntegrationService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final Double backoffFactor;
    private final Duration minInterval;
    private final Duration maxInterval;
    private final Integer multiplierUnit;

    public ReRatePollingScheduler(CronExecutorProperties properties) {
        log.info("Initializing ReRatePollingScheduler with properties: {}", properties);
        this.properties = properties;

        this.workerPool = new ThreadPoolExecutor(
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(properties.getQueueCapacity()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "ReRate-Scheduler"));

        this.minInterval = properties.getMinInterval();
        this.maxInterval = properties.getMaxInterval();
        this.backoffFactor = properties.getBackoffFactor();
        this.currentPollIntervalMs = new AtomicLong(minInterval.toMillis());
        this.multiplierUnit = properties.getMultiplierUnit();

        start();
    }

    public void start() {
        log.info("Starting ReRate polling scheduler with initial interval: {} ms", currentPollIntervalMs.get());
        scheduleNextPoll(currentPollIntervalMs.get());
    }

    private void scheduleNextPoll(long delayMs) {
        scheduler.schedule(this::poll, delayMs, TimeUnit.MILLISECONDS);
    }

    private void poll() {
        try {
            if (workerPool.getActiveCount() >= MAX_THREAD_THRESHOLD) {
                log.debug("Worker pool at capacity, skipping the poll...");
                return;
            }

            boolean foundAny = pollDbForNewRequestsAndSubmit();

            if (foundAny) {
                resetBackoff();
                return;
            }

            applyExponentialBackoff();
        } catch (Exception e) {
            log.error("Exception in poll cycle: {}", e.getMessage(), e);
            handlePollFailure();
        } finally {
            scheduleNextPoll(currentPollIntervalMs.get());
        }
    }

    private void applyExponentialBackoff() {
        int emptyCount = consecutiveEmptyPolls.incrementAndGet();
        boolean reachedMaxInterval = (currentPollIntervalMs.get() >= maxInterval.toMillis());

        if (!reachedMaxInterval) {
            currentPollIntervalMs.updateAndGet(current -> {
                double exponent = (double) emptyCount / multiplierUnit;
                double multiplier = Math.pow(backoffFactor, exponent);
                long nextInterval = (long) (minInterval.toMillis() * multiplier);
                nextInterval = Math.min(nextInterval, maxInterval.toMillis());
                return nextInterval;
            });
            return;
        }

        int attemptsAtMax = attemptsAtMaxInterval.incrementAndGet();
        if (attemptsAtMax >= MAX_ATTEMPTS_AT_MAX_INTERVAL) {
            consecutiveEmptyPolls.set(0);
            attemptsAtMaxInterval.set(0);
            currentPollIntervalMs.set(minInterval.toMillis());
        }
    }

    private void resetBackoff() {
        consecutiveEmptyPolls.set(0);
        attemptsAtMaxInterval.set(0);
        currentPollIntervalMs.set(minInterval.toMillis());
    }

    private void handlePollFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= MAX_CONSECUTIVE_FAILURES) {
            currentPollIntervalMs.set(Duration.ofMinutes(5).toMillis());
            consecutiveFailures.set(0);
        } else {
            currentPollIntervalMs.updateAndGet(current -> Math.min(current * 2, maxInterval.toMillis()));
        }
    }

    private boolean pollDbForNewRequestsAndSubmit() {
        try {
            List<ReRateRequest> requests = transactionTemplate.execute(status -> fetchAndLockPendingRequests());

            if (requests == null || requests.isEmpty()) return false;

            boolean anySubmitted = false;
            for (ReRateRequest request : requests) {
                try {
                    if (workerPool.getActiveCount() >= MAX_THREAD_THRESHOLD) throw new RuntimeException("Worker pool full, stopping submission");
                    submitRequest(request);
                    anySubmitted = true;
                } catch (RejectedExecutionException e) {
                    releaseRequestLock(request.getRequestId());
                } catch (Exception e) {
                    releaseRequestLock(request.getRequestId());
                }
            }
            return anySubmitted;
        } catch (Exception e) {
            log.error("Error polling for requests: {}", e.getMessage(), e);
            return false;
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    protected List<ReRateRequest> fetchAndLockPendingRequests() {
        List<ReRateRequest> candidates = reRateRequestRepository
                .findPendingRequests().parallelStream().limit(MAX_THREAD_THRESHOLD).collect(Collectors.toList());

        if (candidates.isEmpty()) return new ArrayList<>();

        List<ReRateRequest> claimed = new ArrayList<>();
        String instanceId = getInstanceIdentifier();

        for (ReRateRequest request : candidates) {
            try {
                ReRateRequest fresh = reRateRequestRepository.findByRequestIdAndStatus(request.getRequestId(), request.getStatus())
                        .orElse(null);

                if (fresh != null) {
                    fresh.setStatus("IN_PROGRESS");
                    fresh.setModifiedBy(instanceId);
                    fresh.setModifiedAt(LocalDateTime.now());
                    
                    // Initialize lazily loaded query configs before passing out of session
                    if (fresh.getVoiceQueryConfig() != null) fresh.getVoiceQueryConfig().getFetchQuery();
                    if (fresh.getSmsQueryConfig() != null) fresh.getSmsQueryConfig().getFetchQuery();
                    if (fresh.getUsageQueryConfig() != null) fresh.getUsageQueryConfig().getFetchQuery();
                    
                    reRateRequestRepository.save(fresh);
                    claimed.add(fresh);
                }
            } catch (ObjectOptimisticLockingFailureException e) {
                log.debug("Request {} already claimed by another process", request.getRequestId());
            } catch (Exception e) {
                log.error("Error claiming request {}: {}", request.getRequestId(), e.getMessage());
            }
        }
        return claimed;
    }

    private void releaseRequestLock(String requestId) {
        String sql = "UPDATE re_rate_request SET status = 'PENDING' WHERE request_id = ?";
        jdbcTemplate.update(sql, requestId);
    }

    private String getInstanceIdentifier() {
        try {
            String hostname = java.net.InetAddress.getLocalHost().getHostName();
            String pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            if (pid.contains("@")) {
                pid = pid.split("@")[0];
            }
            return hostname + "-" + pid;
        } catch (Exception e) {
            return "instance-" + Thread.currentThread().getId() + "-" + System.currentTimeMillis();
        }
    }

    public void submitRequest(ReRateRequest request) {
        workerPool.setThreadFactory(new ReRatedThreadFactory(request.getRequestId()));
        workerPool.execute(() -> reRateProcess(request));
    }

    private void reRateProcess(ReRateRequest reRateRequest) {
        long startTime = System.currentTimeMillis();
        int totalProcessed = 0;
        try {
            // Build ordered map of non-null query configs: serviceType → queryConfig
            Map<ServiceType, CdrQueryConfig> queryMap = buildQueryMap(reRateRequest);
            if (queryMap.isEmpty()) {
                throw new RuntimeException("No query configs set for Re-Rate Request: " + reRateRequest.getRequestId());
            }

            log.info("Re-Rate :: Starting for request: {} with CDR types: {}", reRateRequest.getRequestId(), queryMap.keySet());

            List<RequestParameters> params = RegistryUtils.convertParameterStringToJson(reRateRequest.getRequestParameters());

            for (Map.Entry<ServiceType, CdrQueryConfig> entry : queryMap.entrySet()) {
                ServiceType serviceType = entry.getKey();
                CdrQueryConfig queryConfig = entry.getValue();

                if (Thread.currentThread().isInterrupted()) break;

                List<RequestParameters> serviceParams = params.stream()
                        .filter(p -> p.getServiceType() == null || p.getServiceType().equals(serviceType.name()))
                        .collect(Collectors.toList());

                try {
                    int offset = 0;
                    AtomicBoolean hasMore = new AtomicBoolean(true);

                    while (hasMore.get() && !Thread.currentThread().isInterrupted()) {
                        List<Long> pageIds = fetchRecordIdsPage(queryConfig.getFetchQuery(), serviceParams, reRateRequest, offset);

                        if (pageIds.isEmpty()) { hasMore.set(false); break; }

                        log.info("Re-Rate :: [{}] Fetched {} IDs (offset: {})", serviceType, pageIds.size(), offset);

                        for (Long id : pageIds) {
                            try {
                                rateRecord(id, serviceType);
                                totalProcessed++;
                            } catch (Exception e) {
                                log.error("Re-Rate :: [{}] Error processing record {}: {}", serviceType, id, e.getMessage());
                            }
                        }
                        offset += PAGE_SIZE;
                        hasMore.set(pageIds.size() == PAGE_SIZE);
                        if (hasMore.get()) Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Re-Rate :: [{}] Failed for request {}: {}", serviceType, reRateRequest.getRequestId(), e.getMessage());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Completed re-rate request {}: processed {} records in {} ms", reRateRequest.getRequestId(), totalProcessed, duration);
            maxRetryForReRateReqProcessing.set(0);
            reRateRequestService.editReRateStatus(reRateRequest.getRequestId(), RequestStatus.COMPLETED.name());
        } catch (Exception e) {
            log.error("Failed re-rate process for request {}: {}", reRateRequest.getRequestId(), e.getMessage(), e);
            maxRetryForReRateReqProcessing.incrementAndGet();
            reRateRequestService.editReRateStatus(reRateRequest.getRequestId(), RequestStatus.FAILED.name());
        }
    }

    private Map<ServiceType, CdrQueryConfig> buildQueryMap(ReRateRequest request) {
        Map<ServiceType, CdrQueryConfig> map = new LinkedHashMap<>();
        if (request.getVoiceQueryConfig() != null) map.put(ServiceType.VOICE, request.getVoiceQueryConfig());
        if (request.getSmsQueryConfig() != null)   map.put(ServiceType.SMS,   request.getSmsQueryConfig());
        if (request.getUsageQueryConfig() != null) map.put(ServiceType.USAGE, request.getUsageQueryConfig());
        return map;
    }

    private List<Long> fetchRecordIdsPage(String fetchQuery, List<RequestParameters> params, ReRateRequest request, int offset) {
        StringBuilder sql = new StringBuilder(fetchQuery);
        List<Object> sqlParams = new ArrayList<>();

        for (RequestParameters param : params) {
            sql.append(" AND ").append(param.getParameterField()).append(" = ?");
            sqlParams.add(param.getParameterValue());
        }
        
        // Add date filters if provided
        if (request.getStartDate() != null) {
            sql.append(" AND start_time >= ?");
            sqlParams.add(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            sql.append(" AND start_time <= ?");
            sqlParams.add(request.getEndDate());
        }

        sql.append(" LIMIT ? OFFSET ?");
        sqlParams.add(PAGE_SIZE);
        sqlParams.add(offset);

        return jdbcTemplate.query(sql.toString(), sqlParams.toArray(), (rs, rowNum) -> rs.getLong(1));
    }

    private void rateRecord(Long id, ServiceType serviceType) {
        switch (serviceType) {
            case VOICE -> voiceRatedCdrRepository.findById(id).ifPresent(cdr -> {
                ratingIntegrationService.applyRating(cdr, cdr.getServiceType(), cdr.getCallType());
                voiceRatedCdrRepository.save(cdr);
            });
            case SMS -> smsRatedCdrRepository.findById(id).ifPresent(cdr -> {
                ratingIntegrationService.applyRating(cdr, cdr.getServiceType(), cdr.getCallType());
                smsRatedCdrRepository.save(cdr);
            });
            case USAGE -> usageRatedCdrRepository.findById(id).ifPresent(cdr -> {
                ratingIntegrationService.applyRating(cdr, cdr.getServiceType(), null);
                usageRatedCdrRepository.save(cdr);
            });
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        workerPool.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) scheduler.shutdownNow();
            if (!workerPool.awaitTermination(60, TimeUnit.SECONDS)) workerPool.shutdownNow();
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
