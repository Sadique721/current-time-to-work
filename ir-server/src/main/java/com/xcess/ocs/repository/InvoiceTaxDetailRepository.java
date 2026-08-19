package com.xcess.ocs.repository;

import com.xcess.ocs.entity.InvoiceTaxDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceTaxDetailRepository extends JpaRepository<InvoiceTaxDetail, Long> {

    List<InvoiceTaxDetail> findByInvoice_InvoiceIdOrderByApplyOrderAsc(Long invoiceId);
}
