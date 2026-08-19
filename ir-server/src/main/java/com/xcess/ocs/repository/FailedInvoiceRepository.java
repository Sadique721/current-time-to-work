package com.xcess.ocs.repository;

import com.xcess.ocs.entity.FailedInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for failed invoices database operations.
 */
@Repository
public interface FailedInvoiceRepository extends JpaRepository<FailedInvoice, Long> {

    List<FailedInvoice> findByAgreementIdOrderByCreatedDateDesc(Long agreementId);

    List<FailedInvoice> findByBillingDateOrderByCreatedDateDesc(java.time.LocalDate billingDate);
}
