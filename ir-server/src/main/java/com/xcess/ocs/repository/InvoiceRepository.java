package com.xcess.ocs.repository;

import com.xcess.ocs.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {
    List<Invoice> findByAgreement_AgreementId(Long agreementId);
    Optional<Invoice> findByAgreement_AgreementIdAndBillingCycleStartAndBillingCycleEnd(
            Long agreementId, LocalDate start, LocalDate end);
    boolean existsByAgreement_AgreementIdAndBillingCycleStartAndBillingCycleEnd(
            Long agreementId, LocalDate start, LocalDate end);
    boolean existsByAgreement_AgreementIdAndBillingCycleStartAndBillingCycleEndAndSettlementType(
            Long agreementId, LocalDate start, LocalDate end, String settlementType);
    long countBySettlementTypeAndBillingCycleStart(
            String settlementType, LocalDate billingCycleStart);
    long countByInvoiceNumberLike(String invoiceNumberPattern);

    @Query("SELECT i FROM Invoice i " +
            "WHERE (:searchTerm IS NULL OR LOWER(i.agreement.agreementCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND (:status IS NULL OR i.status = :status) " +
            "AND (:billingCycleStartFrom IS NULL OR i.billingCycleStart >= :billingCycleStartFrom) " +
            "AND (:billingCycleStartTo IS NULL OR i.billingCycleStart <= :billingCycleStartTo) " +
            "AND (:agreementId IS NULL OR i.agreement.agreementId = :agreementId) " +
            "AND (:taxType IS NULL OR i.taxType = :taxType) " +
            "AND (:taxType IS NULL OR i.taxType = :taxType)")
    Page<Invoice> searchInvoices(
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            @Param("billingCycleStartFrom") LocalDate billingCycleStartFrom,
            @Param("billingCycleStartTo") LocalDate billingCycleStartTo,
            @Param("agreementId") Long agreementId,
            @Param("taxType") String taxType,
            Pageable pageable
    );
}
