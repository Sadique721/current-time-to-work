package com.xcess.ocs.service;

import com.xcess.ocs.dto.FailedInvoiceDTO;
import com.xcess.ocs.entity.FailedInvoice;
import com.xcess.ocs.mapper.FailedInvoiceMapper;
import com.xcess.ocs.repository.FailedInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for managing failed invoices.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FailedInvoiceService {

    private final FailedInvoiceRepository failedInvoiceRepository;
    private final FailedInvoiceMapper failedInvoiceMapper;

    public List<FailedInvoiceDTO> findByAgreementId(Long agreementId) {
        log.debug("Fetching failed invoices for agreement: {}", agreementId);
        return failedInvoiceRepository.findByAgreementIdOrderByCreatedDateDesc(agreementId)
                .stream()
                .map(failedInvoiceMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<FailedInvoiceDTO> findByBillingDate(LocalDate billingDate) {
        log.debug("Fetching failed invoices for billing date: {}", billingDate);
        return failedInvoiceRepository.findByBillingDateOrderByCreatedDateDesc(billingDate)
                .stream()
                .map(failedInvoiceMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<FailedInvoiceDTO> findAll() {
        log.debug("Fetching all failed invoices");
        return failedInvoiceRepository.findAll().stream()
                .map(failedInvoiceMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public FailedInvoiceDTO saveFailedInvoice(Long agreementId, LocalDate billingStartDate,
                                             LocalDate billingEndDate, String errorMessage) {
        log.info("Saving failed invoice for agreement: {}", agreementId);

        FailedInvoice failedInvoice = new FailedInvoice();
        failedInvoice.setAgreementId(agreementId);
        failedInvoice.setBillingStartDate(billingStartDate);
        failedInvoice.setBillingEndDate(billingEndDate);
        failedInvoice.setErrorMessage(errorMessage);
        failedInvoice.setBillingDate(LocalDate.now());

        FailedInvoice saved = failedInvoiceRepository.save(failedInvoice);
        log.info("Failed invoice saved with id: {} for agreement: {}", saved.getId(), agreementId);

        return failedInvoiceMapper.toDto(saved);
    }
}
