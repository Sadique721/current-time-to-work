package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clearing_house_protocols",
       uniqueConstraints = @UniqueConstraint(columnNames = {"clearing_house_id", "protocol"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClearingHouseProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clearing_house_id", nullable = false)
    private ClearingHouse clearingHouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", nullable = false, length = 20)
    private SupportedProtocol protocol;

    public enum SupportedProtocol {
        SFTP, API, AS2
    }
}
