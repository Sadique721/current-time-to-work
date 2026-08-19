package com.xcess.ocs.repository;

import com.xcess.ocs.entity.AccountAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AccountAgreementRepository extends JpaRepository<AccountAgreement, Long> {
    List<AccountAgreement> findByAgreement_AgreementId(Long agreementId);
    void deleteByAgreement_AgreementId(Long agreementId);
}
