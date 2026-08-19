package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tblm_template_configuration", uniqueConstraints = {
    @UniqueConstraint(name = "uk_template_name_deleted_at", columnNames = {"template_name", "deleted_at"})
})
@SQLDelete(sql = "UPDATE tblm_template_configuration SET is_deleted = true, deleted_at = NOW() WHERE template_id = ?")
@Where(clause = "is_deleted = false")
@NoArgsConstructor
@AllArgsConstructor
public class TemplateConfiguration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "template_description")
    private String templateDescription;

    @Column(name = "template_path", nullable = false)
    private String templatePath;

    @Column(name = "template_content_hash", nullable = false, length = 64)
    private String templateContentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_of_business", length = 20)
    private LineOfBusiness lineOfBusiness;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}