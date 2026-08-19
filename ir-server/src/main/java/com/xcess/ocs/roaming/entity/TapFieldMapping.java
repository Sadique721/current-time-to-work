package com.xcess.ocs.roaming.entity;

import com.xcess.ocs.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Schema(description = "Master registry entry for a single GSMA TAP ASN.1 field path and its DTO binding")
@Entity
@Table(name = "tap_field_mappings")
@SQLDelete(sql = "UPDATE tap_field_mappings SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TapFieldMapping extends BaseEntity {

    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Schema(description = "Call type scope. Null means globally applicable to all event types.",
            example = "MO_VOICE", allowableValues = {"GPRS", "MO_VOICE", "MT_VOICE", "MO_SMS", "MT_SMS"})
    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", columnDefinition = "VARCHAR(20)")
    private CallType callType;

    @Schema(description = "Human-readable logical name used in UI and logs", example = "dialledDigits")
    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Schema(description = "Dot-notation ASN.1 path inside the GSMA TAP structure",
            example = "basicCallInformation.destination.dialledDigits")
    @Column(name = "asn_path", nullable = false)
    private String asnPath;

    @Schema(description = "Encoding/decoding data type",
            example = "BCD_STRING", allowableValues = {"BCD_STRING", "ASCII_STRING", "INTEGER", "DECIMAL", "DATE_TIME"})
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, columnDefinition = "VARCHAR(50)")
    private TapDataType dataType;

    @Schema(description = "Property name on RatedCdr to read from during TAP OUT generation", example = "calledNumber")
    @Column(name = "out_source_column")
    private String outSourceColumn;

    @Schema(description = "Property name on TapCdrDTO to write to during TAP IN decoding", example = "calledNumber")
    @Column(name = "in_target_column")
    private String inTargetColumn;

    @Schema(description = "Global fallback value used when the source column resolves to null", example = "00000000000000")
    @Column(name = "default_value")
    private String defaultValue;

    @Schema(description = "If true, TAP file generation or parsing fails when this field cannot be resolved", example = "false")
    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory;
}
