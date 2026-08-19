package com.xcess.ocs.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Organization entity representing a company or business unit.
 * Uses soft delete pattern - records are marked as deleted instead of being physically removed.
 * Inherits isDeleted flag from BaseEntity.
 * 
 * @see BaseEntity
 */
@Getter
@Setter
@Entity
@Table(name = "organizations")
@SQLDelete(sql = "UPDATE organizations SET is_deleted = true, deleted_at = NOW() WHERE organization_id = ?")
@Where(clause = "is_deleted = false")
@NoArgsConstructor
@AllArgsConstructor
public class Organization extends BaseEntity {
    
    /** Unique identifier for the organization */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "organization_id")
    private Long organizationId;

    /** Organization name - must be unique and not blank */
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Optional suffix name for the organization */
    @Size(max = 100, message = "Suffix name must not exceed 100 characters")
    @Column(name = "suffix_name", length = 100)
    private String suffixName;

    /** Legal name of the organization - required for official purposes */
    @NotBlank(message = "Legal name is required")
    @Size(max = 255, message = "Legal name must not exceed 255 characters")
    @Column(name = "legal_name", nullable = false, length = 255)
    private String legalName;

    /** Physical address of the organization */
    @Size(max = 500, message = "Address must not exceed 500 characters")
    @Column(name = "address", length = 500)
    private String address;
}
