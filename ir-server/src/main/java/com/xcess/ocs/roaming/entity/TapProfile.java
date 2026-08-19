package com.xcess.ocs.roaming.entity;

import com.xcess.ocs.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

/**
 * Service type this TAP profile applies to (VOICE, USAGE, SMS, etc.).
 */

/**
 * Named TAP profile template that groups a set of field mapping overrides.
 *
 * <p>A profile acts as a container that selects which fields from the master
 * {@link TapFieldMapping} dictionary are active for a given partner, and optionally
 * overrides their default values or mandatory flags.
 *
 * <p>Profiles are assigned to partners via {@code tap_out_config.tap_profile_id}.
 * If no partner-specific profile is assigned, the system falls back to the
 * default standard profile resolved from the database.
 */
@Schema(description = "Named TAP profile grouping field mapping overrides, assigned to partners via tap_out_config")
@Entity
@Table(name = "tap_profiles")
@SQLDelete(sql = "UPDATE tap_profiles SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TapProfile extends BaseEntity {

    @Schema(description = "Unique identifier", example = "101", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Schema(description = "Unique name identifying this profile", example = "EU_Standard_Profile")
    @Column(name = "profile_name", nullable = false)
    private String profileName;

    @Schema(description = "Human-readable description of the profile's use case",
            example = "Standard field mappings for European Union roaming partners")
    @Column(name = "description")
    private String description;

    @Schema(description = "Whether this profile is active and available for assignment to partners", example = "true")
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Schema(description = "Service type this profile applies to (e.g. VOICE, USAGE, SMS)", example = "VOICE")
    @Column(name = "service_type", length = 50)
    private String serviceType;

    @Schema(description = "Field mapping overrides belonging to this profile",
            accessMode = Schema.AccessMode.READ_ONLY)
    @OneToMany(mappedBy = "profile", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = false, fetch = FetchType.LAZY)
    @Builder.Default
    @BatchSize(size = 50)
    private List<TapProfileFieldMapping> fieldMappings = new ArrayList<>();
}
