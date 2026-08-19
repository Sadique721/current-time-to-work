package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

/**
 * Prefix entity representing area/mobile codes for countries.
 * Uses soft delete pattern - records are marked as deleted instead of being physically removed.
 * Inherits isDeleted flag from BaseEntity.
 * 
 * <p>Note: Same prefix can exist for different countries (e.g., 415 for USA and Canada).</p>
 * <p>Prefix is stored WITHOUT country code (e.g., "415" not "1415").</p>
 * 
 * @see BaseEntity
 * @see Country
 */
@Entity
@Table(name = "prefixes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_country_prefix", columnNames = {"country_id", "prefix", "deleted_at"})
})
@SQLDelete(sql = "UPDATE prefixes SET is_deleted = true, deleted_at = NOW() WHERE prefix_id = ?")
@Where(clause = "is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Prefix extends BaseEntity {

    /** Unique identifier for the prefix */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long prefixId;

    /** Country this prefix belongs to - required for each prefix */
    @ManyToOne
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    /** Area/mobile code WITHOUT country code (e.g., "415", "91", "9876").
     * Only numbers allowed. Duplicate values allowed for different countries.
     * For ROAMING: stores PLMN prefix (e.g. "23801", "26201").
     * For INTERCONNECT: stores area/mobile code (e.g. "415", "9876"). */
    @Column(nullable = false)
    private String prefix;

    /** Descriptive name for the prefix (letters, spaces, hyphens only - no numbers) */
    @Column(nullable = false)
    private String prefixName;

    /**
     * Distinguishes INTERCONNECT (area/mobile codes) from ROAMING (PLMN prefixes).
     * Defaults to INTERCONNECT for backward compatibility with existing rows.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "prefix_type", nullable = false, columnDefinition = "VARCHAR(20)")
    private PrefixType prefixType = PrefixType.INTERCONNECT;
}