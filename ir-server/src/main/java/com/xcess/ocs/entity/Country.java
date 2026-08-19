package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Country entity representing countries for rate management.
 * Uses soft delete pattern - records are marked as deleted instead of being physically removed.
 * Inherits isDeleted flag from BaseEntity.
 * 
 * <p>Note: Same country code can be used by multiple countries (e.g., USA and Canada both use "1",
 * Russia and Kazakhstan both use "7") - this is allowed to support NANP and other shared code systems.</p>
 * 
 * @see BaseEntity
 */
@Entity
@Table(name = "countries", uniqueConstraints = {
    @UniqueConstraint(name = "uk_countries_name", columnNames = {"name", "deleted_at"})
})
@SQLDelete(sql = "UPDATE countries SET is_deleted = true, deleted_at = NOW() WHERE country_id = ?")
@Where(clause = "is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Country extends BaseEntity{

    /** Unique identifier for the country */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "country_id")
    private Long countryId;

    /** Country name - must be unique (case-insensitive) and not blank */
    @Column(name = "name", nullable = false)
    private String name;

    /** Country code without + prefix (e.g., "1", "91", "44").
     * Duplicate codes are allowed for different countries (USA/Canada = "1", Russia/Kazakhstan = "7") */
    @Column(name = "country_code", nullable = false)
    private String countryCode;

    /** ISO 4217 currency code (e.g., "USD", "INR") */
    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    /** Currency symbol (e.g., "$", "₹") */
    @Column(name = "currency_symbol", length = 10)
    private String currencySymbol;

    /** ISO 3166 alpha-2 country code (e.g., "IN", "US", "GB") */
    @Column(name = "iso_code", length = 2, nullable = false)
    private String isoCode;

    /**
     * Constructor for creating Country with only ID.
     * Useful for JPA relationships when only ID is needed.
     * 
     * @param countryId the country ID
     */
    public Country(Long countryId) {
        this.countryId = countryId;
    }
}
