package com.xcess.ocs.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.NotEmpty;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rate_packages", uniqueConstraints = {
    @UniqueConstraint(name = "uk_rate_packages_package_name", columnNames = {"package_name", "deleted_at"})
})
@SQLDelete(sql = "UPDATE rate_packages SET is_deleted = true, deleted_at = NOW() WHERE rate_package_id = ?")
@Where(clause = "is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatePackage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ratePackageId;

    @Column(name = "package_name", nullable = false)
    @NotEmpty(message = "Package name is required")
    private String packageName;

    private String packageDesc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private RatePackageType ratePackageType;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "rate")
    private Double rate;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;

    @ManyToOne
    @JoinColumn(name = "pulse_id", nullable = false)
//    @NotFound(action = NotFoundAction.IGNORE)  // Prevents error if soft deleting Pulse
    private Pulse pulse;

    @Enumerated(EnumType.STRING)
    private Rounding rounding;

    @Enumerated(EnumType.STRING)
    private Rounding priceRounding;

    @OneToMany(mappedBy = "ratePackage", cascade = CascadeType.ALL)
    @JsonManagedReference
    @Builder.Default
    @Fetch(FetchMode.SUBSELECT)
    private List<RateDetails> rateDetails = new ArrayList<>();

    @OneToMany(mappedBy = "ratePackage")
    @Builder.Default
    private List<RatePackageAssociation> ratePackageAssociations = new ArrayList<>();

    @Column(name = "currency", length = 3)
    private String currency;


}
