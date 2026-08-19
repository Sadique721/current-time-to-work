package com.xcess.ocs.repository;

import com.xcess.ocs.entity.Agreement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, Long> {
    Optional<Agreement> findByAgreementCode(String agreementCode);
    boolean existsByAgreementCode(String agreementCode);
    Page<Agreement> findByAgreementCodeContainingIgnoreCase(String searchTerm, Pageable pageable);

    /**
     * Counts the number of non-deleted agreements using a specific incoming settlement template.
     * @param templateId The ID of the template
     * @return Count of agreements using this incoming template
     */
    long countByIncomingSettlementTemplate_TemplateIdAndIsDeletedFalse(Long templateId);

    /**
     * Counts the number of non-deleted agreements using a specific outgoing settlement template.
     * @param templateId The ID of the template
     * @return Count of agreements using this outgoing template
     */
    long countByOutgoingSettlementTemplate_TemplateIdAndIsDeletedFalse(Long templateId);

    /**
     * Counts the number of non-deleted agreements using a specific net settlement template.
     * @param templateId The ID of the template
     * @return Count of agreements using this net template
     */
    long countByNetSettlementTemplate_TemplateIdAndIsDeletedFalse(Long templateId);

    @Query("SELECT DISTINCT a FROM Agreement a LEFT JOIN FETCH a.accountAgreements WHERE a.agreementId = :id")
    Optional<Agreement> findByIdWithAccountAgreements(@Param("id") Long id);

    /**
     * Find the active ROAMING agreement for a partner.
     * Used by RoamingInvoiceService to resolve tax configs for TAP IN invoice generation.
     */
    @Query("SELECT a FROM Agreement a JOIN a.accountAgreements aa " +
           "WHERE aa.account.partner.partnerId = :partnerId " +
           "AND a.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.ROAMING " +
           "AND a.isDeleted = false")
    Optional<Agreement> findActiveRoamingAgreementByPartnerId(@Param("partnerId") Long partnerId);
}
