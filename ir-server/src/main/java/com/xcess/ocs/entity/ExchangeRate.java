package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "exchange_rates")
@SQLDelete(sql = "UPDATE exchange_rates SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** ISO 4217 currency code — the currency being converted from (e.g. "USD") */
    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    /** ISO 4217 currency code — the currency being converted to (e.g. "INR") */
    @Column(name = "target_currency", nullable = false, length = 3)
    private String targetCurrency;

    /** Conversion rate: 1 base_currency = rate target_currency */
    @Column(name = "rate", nullable = false, precision = 18, scale = 6)
    private BigDecimal rate;

    /** Date from which this rate is valid */
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    /** The provider/feed from where this rate was fetched (e.g. "ECB", "XE", "MANUAL") */
    @Column(name = "source", nullable = false, length = 50)
    private String source;

    /** Decimal precision of the target currency */
    @Column(name = "target_currency_precision", nullable = false)
    @Builder.Default
    private Integer targetCurrencyPrecision = 2;
}
