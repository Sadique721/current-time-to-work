package com.xcess.ocs.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "rate_details_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateDetailsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @Column(name = "rate_details_id")
    private Long rateDetailsId;

    @Column(name = "destination_prefix")
    private String destinationPrefix;

    @Column(name = "destination_prefix_name")
    private String destinationPrefixName;

    @Column(name = "source_prefix")
    private String sourcePrefix;

    @Column(name = "source_prefix_name")
    private String sourcePrefixName;

    /** ROAMING: zone name this rate applies to (e.g. "South Asia", "Europe"). */
    @Column(name = "zone_name")
    private String zoneName;

    private Double rate;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer versionNumber;

    @Column(name = "rate_package_id", nullable = false)
    private Long ratePackageId;

    @Column(name = "source_country_code")
    private String sourceCountryCode;

    @Column(name = "source_country_name")
    private String sourceCountryName;

    @Column(name = "destination_country_code")
    private String destinationCountryCode;

    @Column(name = "destination_country_name")
    private String destinationCountryName;

    @ManyToOne
    @JoinColumn(name = "source_country_id")
    private Country sourceCountryId;

    @ManyToOne
    @JoinColumn(name = "destination_country_id")
    private Country destinationCountryId;

    @CreatedDate
    @Column(updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;

    @LastModifiedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifiedDate;
}
