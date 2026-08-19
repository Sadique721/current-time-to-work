package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "states", uniqueConstraints = {
    @UniqueConstraint(name = "uk_states_country_state", columnNames = {"country_iso", "state_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class State {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "state_id")
    private Long stateId;

    @Column(name = "country_iso", nullable = false, length = 2)
    private String countryIso;

    @Column(name = "state_code", nullable = false, length = 10)
    private String stateCode;

    @Column(name = "state_name", nullable = false, length = 100)
    private String stateName;
}
