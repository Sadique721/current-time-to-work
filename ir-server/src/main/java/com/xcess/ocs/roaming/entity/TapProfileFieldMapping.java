package com.xcess.ocs.roaming.entity;

import com.xcess.ocs.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Intersection record linking a {@link TapProfile} to a {@link TapFieldMapping}.
 *
 * <p>Resolves the many-to-many relationship between profiles and master fields.
 * Allows per-profile customisation of the master field's default value and
 * mandatory constraint without modifying the master dictionary.
 *
 * <p>A unique constraint on {@code (profile_id, tap_field_mapping_id)} ensures
 * each field is configured at most once per profile.
 */
@Schema(description = "Intersection record linking a TAP profile to a master field mapping with optional overrides")
@Entity
@Table(name = "tap_profile_field_mappings")
@SQLDelete(sql = "UPDATE tap_profile_field_mappings SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TapProfileFieldMapping extends BaseEntity {

    @Schema(description = "Unique identifier", example = "501", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Schema(description = "The profile this override belongs to", accessMode = Schema.AccessMode.READ_ONLY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private TapProfile profile;

    @Schema(description = "The master field mapping this override is linked to")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tap_field_mapping_id", nullable = false)
    private TapFieldMapping tapFieldMapping;

    @Schema(description = "Overrides the master default_value for this profile only. Null means use master value.",
            example = "99999999999999")
    @Column(name = "custom_default_value")
    private String customDefaultValue;

    @Schema(description = "Overrides the master is_mandatory flag for this profile only. Null means use master value.",
            example = "true")
    @Column(name = "is_mandatory_override")
    private Boolean isMandatoryOverride;
}
