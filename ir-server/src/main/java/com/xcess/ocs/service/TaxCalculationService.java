package com.xcess.ocs.service;

import com.xcess.ocs.config.RoundingProperties;
import com.xcess.ocs.entity.AgreementTaxConfig;
import com.xcess.ocs.entity.TaxConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaxCalculationService {

    private final RoundingProperties roundingProperties;

    public MultiTaxCalculationResult calculateMultiTax(
            BigDecimal baseAmount,
            List<AgreementTaxConfig> agreementTaxConfigs,
            LocalDate invoiceDate) {

        if (baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return calculateExempt(baseAmount);
        }

        int calcPrecision = roundingProperties.getCalculationPrecision();
        int displayPrecision = roundingProperties.getDisplayPrecision();
        RoundingMode roundingMode = roundingProperties.getRoundingMode();

        Map<Integer, BigDecimal> completedOrders = new LinkedHashMap<>();
        List<InvoiceTaxLineItem> lineItems = new ArrayList<>();
        BigDecimal runningTotal = BigDecimal.ZERO;

        List<AgreementTaxConfig> sorted = agreementTaxConfigs.stream()
                .sorted(Comparator.comparingInt(AgreementTaxConfig::getApplyOrder))
                .toList();

        for (AgreementTaxConfig atc : sorted) {
            TaxConfig tc = atc.getTaxConfig();

            if (invoiceDate != null) {
                if (invoiceDate.isBefore(tc.getEffectiveFrom()) ||
                    (tc.getEffectiveTo() != null && invoiceDate.isAfter(tc.getEffectiveTo()))) {
                    throw new RuntimeException("Tax config '" + tc.getTaxName() + "' not effective on " + invoiceDate);
                }
            }

            BigDecimal taxableBase;
            if (atc.getAccumulateFromOrders() == null || atc.getAccumulateFromOrders().isBlank()) {
                taxableBase = baseAmount;
            } else {
                BigDecimal accumulated = BigDecimal.ZERO;
                for (String orderRef : atc.getAccumulateFromOrders().split(",")) {
                    int ref = Integer.parseInt(orderRef.trim());
                    BigDecimal priorTax = completedOrders.get(ref);
                    if (priorTax == null) {
                        throw new RuntimeException("Order " + ref + " referenced but not found");
                    }
                    accumulated = accumulated.add(priorTax);
                }
                taxableBase = baseAmount.add(accumulated);
            }

            BigDecimal rate = tc.getStandardRate().divide(new BigDecimal("100"), 10, roundingMode);
            BigDecimal rawTax = taxableBase.multiply(rate);
            BigDecimal taxAmount = rawTax.setScale(calcPrecision, roundingMode);

            completedOrders.put(atc.getApplyOrder(), taxAmount);
            lineItems.add(new InvoiceTaxLineItem(
                    atc.getApplyOrder(),
                    tc.getTaxConfigId(),
                    tc.getTaxType(),
                    tc.getStandardRate(),
                    taxableBase.setScale(calcPrecision, roundingMode),
                    taxAmount,
                    tc.getApplyOn(),
                    atc.getAccumulateFromOrders()
            ));
            runningTotal = runningTotal.add(taxAmount);
        }

        BigDecimal totalTax = runningTotal.setScale(displayPrecision, roundingMode);
        BigDecimal displayBase = baseAmount.setScale(displayPrecision, roundingMode);
        BigDecimal totalInvoice = baseAmount.add(runningTotal).setScale(displayPrecision, roundingMode);

        return new MultiTaxCalculationResult(
                displayBase,
                totalTax,
                totalInvoice,
                lineItems,
                false,
                invoiceDate);
    }

    public MultiTaxCalculationResult calculateExempt(BigDecimal baseAmount) {
        int displayPrecision = roundingProperties.getDisplayPrecision();
        BigDecimal displayBase = baseAmount == null ? BigDecimal.ZERO : baseAmount;
        return new MultiTaxCalculationResult(
                displayBase.setScale(displayPrecision, roundingProperties.getRoundingMode()),
                BigDecimal.ZERO.setScale(displayPrecision, roundingProperties.getRoundingMode()),
                displayBase.setScale(displayPrecision, roundingProperties.getRoundingMode()),
                Collections.emptyList(),
                false,
                null);
    }

    public static class TaxCalculationResult {
        private final BigDecimal taxableAmount;
        private final BigDecimal taxAmount;
        private final BigDecimal taxRate;
        private final String taxType;

        public TaxCalculationResult(BigDecimal taxableAmount, BigDecimal taxAmount,
                                    BigDecimal taxRate, String taxType) {
            this.taxableAmount = taxableAmount;
            this.taxAmount = taxAmount;
            this.taxRate = taxRate;
            this.taxType = taxType;
        }

        public BigDecimal getTaxableAmount() { return taxableAmount; }
        public BigDecimal getTaxAmount() { return taxAmount; }
        public BigDecimal getTaxRate() { return taxRate; }
        public String getTaxType() { return taxType; }
    }

    public static class MultiTaxCalculationResult {
        private final BigDecimal taxableAmount;
        private final BigDecimal totalTax;
        private final BigDecimal totalInvoiceAmount;
        private final List<InvoiceTaxLineItem> lineItems;
        private final boolean exportTreatment;
        private final LocalDate taxCalculationDate;

        public MultiTaxCalculationResult(BigDecimal taxableAmount, BigDecimal totalTax,
                                         BigDecimal totalInvoiceAmount, List<InvoiceTaxLineItem> lineItems,
                                         boolean exportTreatment, LocalDate taxCalculationDate) {
            this.taxableAmount = taxableAmount;
            this.totalTax = totalTax;
            this.totalInvoiceAmount = totalInvoiceAmount;
            this.lineItems = lineItems;
            this.exportTreatment = exportTreatment;
            this.taxCalculationDate = taxCalculationDate;
        }

        public BigDecimal getTaxableAmount() { return taxableAmount; }
        public BigDecimal getTotalTax() { return totalTax; }
        public BigDecimal getTotalInvoiceAmount() { return totalInvoiceAmount; }
        public List<InvoiceTaxLineItem> getLineItems() { return lineItems; }
        public boolean isExportTreatment() { return exportTreatment; }
        public LocalDate getTaxCalculationDate() { return taxCalculationDate; }
    }

    public static class InvoiceTaxLineItem {
        private final int applyOrder;
        private final Long taxConfigId;
        private final String taxType;
        private final BigDecimal taxRate;
        private final BigDecimal taxableAmount;
        private final BigDecimal taxAmount;
        private final String applyOn;
        private final String accumulateFromOrders;

        public InvoiceTaxLineItem(int applyOrder, Long taxConfigId, String taxType,
                                  BigDecimal taxRate, BigDecimal taxableAmount, BigDecimal taxAmount,
                                  String applyOn, String accumulateFromOrders) {
            this.applyOrder = applyOrder;
            this.taxConfigId = taxConfigId;
            this.taxType = taxType;
            this.taxRate = taxRate;
            this.taxableAmount = taxableAmount;
            this.taxAmount = taxAmount;
            this.applyOn = applyOn;
            this.accumulateFromOrders = accumulateFromOrders;
        }

        public int getApplyOrder() { return applyOrder; }
        public Long getTaxConfigId() { return taxConfigId; }
        public String getTaxType() { return taxType; }
        public BigDecimal getTaxRate() { return taxRate; }
        public BigDecimal getTaxableAmount() { return taxableAmount; }
        public BigDecimal getTaxAmount() { return taxAmount; }
        public String getApplyOn() { return applyOn; }
        public String getAccumulateFromOrders() { return accumulateFromOrders; }
    }
}
