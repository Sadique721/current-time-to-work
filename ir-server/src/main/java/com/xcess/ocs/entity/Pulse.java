package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "pulse_config", uniqueConstraints = {
    @UniqueConstraint(name = "uk_pulse_config_pulse_name", columnNames = {"pulse_name", "deleted_at"})
})
@SQLDelete(sql = "UPDATE pulse_config SET is_deleted = true, deleted_at = NOW() WHERE pulse_id = ?")
@Where(clause = "is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pulse extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pulseId;

    private String pulseName;

    @Enumerated(EnumType.STRING)
    private ServiceType serviceType; // VOICE, SMS, USAGE

    @Enumerated(EnumType.STRING)
    private UnitType unit; // SECOND, MINUTE, EVENT, KB, MB, GB, BYTE

    private int noOfUnits;
}