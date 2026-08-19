package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Data
@Entity
@Table(name = "source_configuration", uniqueConstraints = {
    @UniqueConstraint(name = "uk_source_configuration_topic_name", columnNames = {"topic_name", "deleted_at"})
})
@SQLDelete(sql = "UPDATE source_configuration SET is_deleted = true, deleted_at = NOW() WHERE source_id = ?")
@Where(clause = "is_deleted = false")
public class SourceConfiguration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sourceId;

    @Column(nullable = false)
    private String sourceName;

    @Column(nullable = false)
    private String topicName;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType = ServiceType.VOICE;

    @Column(nullable = false)
    private String status;  // "enabled" or "disabled"

}
