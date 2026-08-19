package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Data
@Entity
@Table(name = "source_cdr_configuration", uniqueConstraints = {
    @UniqueConstraint(name = "uk_source_cdr_config", columnNames = {"source_id", "field_name", "deleted_at"})
})
@SQLDelete(sql = "UPDATE source_cdr_configuration SET is_deleted = true, deleted_at = NOW() WHERE source_cdr_configuration_id = ?")
@Where(clause = "is_deleted = false")
public class SourceCdrConfiguration extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "source_cdr_configuration_id")
    private Long sourceCdrConfigurationId;

    @ManyToOne
    @JoinColumn(name = "source_id", nullable = false)
    private SourceConfiguration sourceConfiguration;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

}

