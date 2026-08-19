package com.xcess.ocs.repository;

import com.xcess.ocs.entity.AgreementTaxConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementTaxConfigRepository extends JpaRepository<AgreementTaxConfig, Long> {

    List<AgreementTaxConfig> findByAgreement_AgreementIdOrderByApplyOrderAsc(Long agreementId);

    void deleteByAgreement_AgreementId(Long agreementId);
}
