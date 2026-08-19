package com.xcess.ocs.service;

import com.xcess.ocs.dto.ExchangeRateDTO;
import com.xcess.ocs.dto.search.ExchangeRateSearchDTO;
import com.xcess.ocs.entity.ExchangeRate;
import com.xcess.ocs.mapper.ExchangeRateMapper;
import com.xcess.ocs.repository.CountryRepository;
import com.xcess.ocs.repository.ExchangeRateRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateMapper exchangeRateMapper;
    private final CountryRepository countryRepository;
    private RestTemplate restTemplate;

    @Value("${exchangeRate.api.url}")
    private String apiUrl;

    @Value("${exchangeRate.base-currencies:INR,USD}")
    private String baseCurrenciesString;

    @Value("${exchangeRate.api.timeout:10000}")
    private int apiTimeout;

    @Value("${exchangeRate.rate-precision:6}")
    private int ratePrecision;

    public ExchangeRateService(ExchangeRateRepository exchangeRateRepository,
                                ExchangeRateMapper exchangeRateMapper,
                                CountryRepository countryRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.exchangeRateMapper = exchangeRateMapper;
        this.countryRepository = countryRepository;
    }

    @PostConstruct
    private void init() {
        this.restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(apiTimeout))
                .setReadTimeout(Duration.ofMillis(apiTimeout))
                .build();
    }

    @Transactional
    public int fetchAndSaveRates() {
        List<String> currencies = Arrays.stream(baseCurrenciesString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        int totalSaved = 0;
        LocalDate today = LocalDate.now();

        Set<String> activeCurrencies = countryRepository.findDistinctCurrencyCodes().stream()
                .filter(c -> c != null && !c.trim().isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        for (String base : currencies) {
            try {
                String resolvedUrl = apiUrl.replace("{baseCurrency}", base);
                log.info("Fetching exchange rates for base currency {} from: {}", base, resolvedUrl);

                FrankfurterResponse response = restTemplate.getForObject(resolvedUrl, FrankfurterResponse.class);

                if (response == null || response.getRates() == null || response.getRates().isEmpty()) {
                    throw new RuntimeException("Empty response from API");
                }

                Map<String, BigDecimal> validRates = filterISO4217(response.getRates(), activeCurrencies);
                log.info("Fetched {} rates for base currency {}, {} valid after dynamic ISO 4217 country filter",
                        response.getRates().size(), base, validRates.size());

                List<ExchangeRate> entities = validRates.entrySet().stream()
                        .map(entry -> {
                            String target = entry.getKey();
                            return ExchangeRate.builder()
                                    .baseCurrency(base)
                                    .targetCurrency(target)
                                    .rate(entry.getValue().setScale(ratePrecision, RoundingMode.HALF_UP))
                                    .targetCurrencyPrecision(getCurrencyPrecision(target))
                                    .validFrom(today)
                                    .source("FRANKFURTER")
                                    .build();
                        })
                        .collect(Collectors.toList());

                List<ExchangeRate> saved = exchangeRateRepository.saveAll(entities);
                log.info("Saved {} exchange rates for base currency {} on {}", saved.size(), base, today);
                totalSaved += saved.size();

            } catch (Exception e) {
                log.warn("API fetch failed for base currency {} (error: {}). Attempting fallback to previous day rates...", 
                        base, e.getMessage());
                
                int fallbackCount = applyFallbackRatesForBaseCurrency(base, today);
                if (fallbackCount == 0) {
                    log.error("Fallback failed for base currency {}: No previous day rates available in database.", base);
                    throw new RuntimeException("Failed to fetch rates from API and no fallback rates found in database for base currency: " + base, e);
                }
                totalSaved += fallbackCount;
            }
        }
        return totalSaved;
    }

    @Transactional
    public int applyFallbackRatesForBaseCurrency(String baseCurrency, LocalDate today) {
        LocalDate maxDate = exchangeRateRepository.findMaxValidFromByBaseCurrency(baseCurrency);
        if (maxDate == null) {
            return 0;
        }

        List<ExchangeRate> latestRates = exchangeRateRepository.findByBaseCurrencyAndValidFrom(baseCurrency, maxDate);
        if (latestRates.isEmpty()) {
            return 0;
        }

        log.info("Applying fallback: cloning {} rates for base currency {} from date {}", 
                latestRates.size(), baseCurrency, maxDate);

        List<ExchangeRate> fallbackRates = latestRates.stream()
                .map(rate -> ExchangeRate.builder()
                        .baseCurrency(baseCurrency)
                        .targetCurrency(rate.getTargetCurrency())
                        .rate(rate.getRate())
                        .targetCurrencyPrecision(rate.getTargetCurrencyPrecision() != null ? rate.getTargetCurrencyPrecision() : getCurrencyPrecision(rate.getTargetCurrency()))
                        .validFrom(today)
                        .source("FALLBACK_PREVIOUS_DAY")
                        .build())
                .collect(Collectors.toList());

        List<ExchangeRate> saved = exchangeRateRepository.saveAll(fallbackRates);
        return saved.size();
    }

    public static int getCurrencyPrecision(String currencyCode) {
        if (currencyCode == null) return 2;
        try {
            java.util.Currency currency = java.util.Currency.getInstance(currencyCode.toUpperCase());
            int fractionDigits = currency.getDefaultFractionDigits();
            // If standard fraction digits is -1 (e.g. for SDR/XDR which have no standard minor unit), fallback to 2
            return fractionDigits >= 0 ? fractionDigits : 2;
        } catch (IllegalArgumentException e) {
            // Fallback for custom or invalid currency codes
            return 2;
        }
    }

    @Transactional(readOnly = true)
    public Page<ExchangeRateDTO> getRatesPaginated(Pageable pageable) {
        return exchangeRateRepository.findAllByOrderByValidFromDescIdDesc(pageable)
                .map(exchangeRateMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ExchangeRateDTO> searchExchangeRates(ExchangeRateSearchDTO searchDTO, Pageable pageable) {
        String searchTerm = searchDTO != null ? searchDTO.getSearchTerm() : null;
        LocalDate validFrom = searchDTO != null ? searchDTO.getValidFrom() : null;
        return exchangeRateRepository.searchExchangeRates(searchTerm, validFrom, pageable)
                .map(exchangeRateMapper::toDto);
    }

    private Map<String, BigDecimal> filterISO4217(Map<String, BigDecimal> rates, Set<String> activeCurrencies) {
        if (rates == null) {
            return Collections.emptyMap();
        }
        return rates.entrySet().stream()
                .filter(entry -> activeCurrencies.contains(entry.getKey()))
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static class FrankfurterResponse {
        private String amount;
        private String base;
        private String date;
        private Map<String, BigDecimal> rates;

        public Map<String, BigDecimal> getRates() { return rates; }
        public void setRates(Map<String, BigDecimal> rates) { this.rates = rates; }
    }
}
