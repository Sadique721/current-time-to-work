package com.xcess.ocs.summaryengine.cron;

import com.xcess.ocs.dto.BillingSchedulerStatusDTO;
import com.xcess.ocs.entity.Agreement;
import com.xcess.ocs.entity.BillingSchedulerStatus;
import com.xcess.ocs.entity.Invoice;
import com.xcess.ocs.entity.LineOfBusiness;
import com.xcess.ocs.repository.AgreementRepository;
import com.xcess.ocs.repository.InvoiceRepository;
import com.xcess.ocs.roaming.entity.TapDirection;
import com.xcess.ocs.roaming.service.RoamingTapOutInvoiceService;
import com.xcess.ocs.service.BillingCycleCalculatorService;
import com.xcess.ocs.service.BillingCycleResult;
import com.xcess.ocs.service.BillingSchedulerAuditLogService;
import com.xcess.ocs.service.BillingSchedulerStatusService;
import com.xcess.ocs.service.FailedInvoiceService;
import com.xcess.ocs.service.InvoiceGenerationService;
import com.xcess.ocs.constants.enums.SettlementType;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler for generating invoices based on billing cycles.
 * Runs daily at midnight and processes all agreements with completed billing cycles.
 */
@Slf4j
@Component
public class BillingCycleScheduler {

    private static final long WAIT_TIMEOUT_MINUTES = 60;
    private static final long WAIT_INTERVAL_SECONDS = 30;

    @Autowired
    private AgreementRepository agreementRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceGenerationService invoiceGenerationService;

    @Autowired
    private BillingSchedulerStatusService billingSchedulerStatusService;

    @Autowired
    private BillingSchedulerAuditLogService billingSchedulerAuditLogService;

    @Autowired
    private FailedInvoiceService failedInvoiceService;

    @Autowired
    private RoamingTapOutInvoiceService roamingTapOutInvoiceService;

    @Autowired
    private BillingCycleCalculatorService billingCycleCalculatorService;

    private volatile boolean interrupted = false;

    @Scheduled(cron = "${billingCycleCronScheduler}")
    public void generateInvoices() {
        log.info("Starting billing cycle invoice generation at midnight");
        long startTime = System.currentTimeMillis();
        int totalProcessed = 0;
        int totalFailed = 0;
        Long schedulerStatusId = null;

        try {
            schedulerStatusId = waitForSchedulerLock();
            if (schedulerStatusId == null) {
                log.warn("Scheduler skipped due to timeout waiting for previous run");
                return;
            }

            billingSchedulerStatusService.updateToRunning();
            billingSchedulerAuditLogService.logSchedulerStarted(schedulerStatusId,
                    "Billing scheduler execution started");

            List<Agreement> agreements = agreementRepository.findAll();
            LocalDate today = LocalDate.now();
            log.info("Found {} agreements to process", agreements.size());

            for (Agreement agreement : agreements) {
                if (interrupted) {
                    log.warn("Scheduler interrupted during processing");
                    break;
                }

                try {
                    boolean processed = processAgreement(agreement, today);
                    if (processed) {
                        totalProcessed++;
                    }
                } catch (Exception e) {
                    totalFailed++;
                    log.error("Error processing agreement {}: {}",
                            agreement.getAgreementCode(), e.getMessage(), e);
                    failedInvoiceService.saveFailedInvoice(
                            agreement.getAgreementId(),
                            agreement.getNextBillingCycleStartDate() != null
                                    ? agreement.getNextBillingCycleStartDate()
                                    : agreement.getBillingCycleStartDate(),
                            estimateCycleEnd(agreement),
                            "Error: " + e.getMessage());
                }
            }

            long executionTime = System.currentTimeMillis() - startTime;

            if (interrupted) {
                billingSchedulerStatusService.updateToInterrupted();
                billingSchedulerAuditLogService.logSchedulerInterrupted(schedulerStatusId,
                        (long) totalProcessed, (long) totalFailed, executionTime,
                        "Scheduler interrupted by application shutdown");
            } else {
                billingSchedulerStatusService.updateToSuccess();
                billingSchedulerAuditLogService.logSchedulerCompleted(schedulerStatusId,
                        (long) totalProcessed, (long) totalFailed, executionTime,
                        String.format("Billing scheduler completed. Processed: %d, Failed: %d",
                                totalProcessed, totalFailed));
            }

            log.info("Billing cycle invoice generation completed. Processed: {}, Failed: {}, Time: {}ms",
                    totalProcessed, totalFailed, executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Billing scheduler execution failed: {}", e.getMessage(), e);

            billingSchedulerStatusService.updateToFailed();
            if (schedulerStatusId != null) {
                billingSchedulerAuditLogService.logSchedulerFailed(schedulerStatusId,
                        (long) totalProcessed, (long) totalFailed, executionTime,
                        "Scheduler execution failed: " + e.getMessage());
            }
        }
    }

    private LocalDate estimateCycleEnd(Agreement agreement) {
        try {
            LocalDate start = agreement.getNextBillingCycleStartDate() != null
                    ? agreement.getNextBillingCycleStartDate()
                    : agreement.getBillingCycleStartDate();
            if (start == null) return LocalDate.now();
            return billingCycleCalculatorService.calculate(start, agreement).cycleEnd();
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private Long waitForSchedulerLock() {
        log.debug("Checking if previous scheduler is still running");

        int waitCount = 0;
        long maxWaitCount = WAIT_TIMEOUT_MINUTES * 60 / WAIT_INTERVAL_SECONDS;

        while (waitCount < maxWaitCount) {
            BillingSchedulerStatusDTO status = billingSchedulerStatusService.getLatestStatus();

            if (status == null || status.getStatus() != BillingSchedulerStatus.Status.RUNNING) {
                log.debug("Scheduler is not running, proceeding with execution");
                BillingSchedulerStatusDTO currentStatus = billingSchedulerStatusService.getOrCreateStatus();
                return currentStatus.getSchedulerStatusId();
            }

            waitCount++;
            log.debug("Previous scheduler still running. Waiting... (attempt {}/{})",
                    waitCount, maxWaitCount);

            try {
                TimeUnit.SECONDS.sleep(WAIT_INTERVAL_SECONDS);
            } catch (InterruptedException e) {
                log.warn("Wait interrupted");
                Thread.currentThread().interrupt();
                return null;
            }
        }

        log.warn("Timeout waiting for previous scheduler to complete. Skipping this execution.");
        return null;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public boolean processAgreement(Agreement agreement, LocalDate today) {
        Agreement managedAgreement = agreementRepository.findByIdWithAccountAgreements(agreement.getAgreementId())
                .orElseThrow(() -> new RuntimeException("Agreement not found: " + agreement.getAgreementId()));

        String agreementCode = managedAgreement.getAgreementCode();
        log.debug("Processing agreement: {}", agreementCode);

        if (managedAgreement.getBillingType() == com.xcess.ocs.entity.BillingType.DAYS
                && managedAgreement.getBillingCyclePeriod() == null) {
            log.warn("SKIP: Agreement {} is DAYS billing type but has null billingCyclePeriod", agreementCode);
            return false;
        }

        if (managedAgreement.getAccountAgreements() == null || managedAgreement.getAccountAgreements().isEmpty()) {
            log.warn("SKIP: Agreement {} has no mapped accounts", agreementCode);
            return false;
        }

        LocalDate cycleStart = managedAgreement.getNextBillingCycleStartDate();
        if (cycleStart == null) {
            cycleStart = managedAgreement.getBillingCycleStartDate();
            log.info("Agreement {} has null nextBillingCycleStartDate, using billingCycleStartDate: {}",
                    agreementCode, cycleStart);
        }

        if (cycleStart == null) {
            log.warn("SKIP: Agreement {} has null billingCycleStartDate", agreementCode);
            return false;
        }

        if (cycleStart.isAfter(today)) {
            log.debug("SKIP: Agreement {} next billing date is in future: {}", agreementCode, cycleStart);
            return false;
        }

        com.xcess.ocs.service.BillingCycleResult cycleResult = billingCycleCalculatorService.calculate(cycleStart, managedAgreement);
        LocalDate cycleEnd = cycleResult.cycleEnd();

        if (!cycleEnd.isBefore(today)) {
            log.debug("SKIP: Agreement {} cycle not yet complete (ends: {})", agreementCode, cycleEnd);
            return false;
        }

        // Process each enabled settlement type
        boolean success = true;

        // INTERCONNECT invoice
        if(managedAgreement.getLineOfBusiness().equals(LineOfBusiness.INTERCONNECT)) {
            // INCOMING settlement - for CUSTOMER partners
            if (Boolean.TRUE.equals(managedAgreement.getIsIncomingSettlement())) {
                if (!invoiceRepository.existsByAgreement_AgreementIdAndBillingCycleStartAndBillingCycleEndAndSettlementType(
                        managedAgreement.getAgreementId(), cycleStart, cycleEnd, SettlementType.INCOMING.label())) {
                    Invoice invoice = invoiceGenerationService.generateInvoiceForSettlementType(
                            managedAgreement.getAgreementId(), cycleStart, cycleEnd, SettlementType.INCOMING.label());
                    if (invoice == null) {
                        failedInvoiceService.saveFailedInvoice(managedAgreement.getAgreementId(), cycleStart, cycleEnd,
                                "INCOMING: Invoice returned is null");
                        success = false;
                    }
                }
            }

            // OUTGOING settlement - for VENDOR partners
            if (Boolean.TRUE.equals(managedAgreement.getIsOutgoingSettlement())) {
                if (!invoiceRepository.existsByAgreement_AgreementIdAndBillingCycleStartAndBillingCycleEndAndSettlementType(
                        managedAgreement.getAgreementId(), cycleStart, cycleEnd, SettlementType.OUTGOING.label())) {
                    Invoice invoice = invoiceGenerationService.generateInvoiceForSettlementType(
                            managedAgreement.getAgreementId(), cycleStart, cycleEnd, SettlementType.OUTGOING.label());
                    if (invoice == null) {
                        failedInvoiceService.saveFailedInvoice(managedAgreement.getAgreementId(), cycleStart, cycleEnd,
                                "OUTGOING: Invoice returned is null");
                        success = false;
                    }
                }
            }

            // NET settlement - for both
            if (Boolean.TRUE.equals(managedAgreement.getIsNetSettlement())) {
                if (!invoiceRepository.existsByAgreement_AgreementIdAndBillingCycleStartAndBillingCycleEndAndSettlementType(
                        managedAgreement.getAgreementId(), cycleStart, cycleEnd, SettlementType.NET.label())) {
                    Invoice invoice = invoiceGenerationService.generateInvoiceForSettlementType(
                            managedAgreement.getAgreementId(), cycleStart, cycleEnd, SettlementType.NET.label());
                    if (invoice == null) {
                        failedInvoiceService.saveFailedInvoice(managedAgreement.getAgreementId(), cycleStart, cycleEnd,
                                "NET: Invoice returned is null");
                        success = false;
                    }
                }
            }
        }
        // ROAMING TAP OUT invoice
        if (managedAgreement.getLineOfBusiness() == LineOfBusiness.ROAMING
                && managedAgreement.getTapDirection() == TapDirection.TAP_OUT) {
            if (!invoiceRepository.existsByAgreement_AgreementIdAndBillingCycleStartAndBillingCycleEndAndSettlementType(
                    managedAgreement.getAgreementId(), cycleStart, cycleEnd, RoamingTapOutInvoiceService.SETTLEMENT_TYPE)) {
                Invoice invoice = roamingTapOutInvoiceService.generateInvoice(
                        managedAgreement.getAgreementId(), cycleStart, cycleEnd);
                if (invoice == null) {
                    failedInvoiceService.saveFailedInvoice(managedAgreement.getAgreementId(), cycleStart, cycleEnd,
                            "ROAMING_TAP_OUT: Invoice returned is null");
                    success = false;
                }
            }
        }

        if (success) {
            managedAgreement.setNextBillingCycleStartDate(cycleResult.nextCycleStart());
            agreementRepository.save(managedAgreement);
        }

        return success;
    }

    @PreDestroy
    public void onShutdown() {
        log.warn("Application shutdown detected. Marking scheduler as interrupted.");
        interrupted = true;
    }
}
