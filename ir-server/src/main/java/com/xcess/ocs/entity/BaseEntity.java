package com.xcess.ocs.entity;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;



//@SQLDelete(sql = "UPDATE ${tableName} SET is_deleted = true WHERE id = ?")
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
//@FilterDef(name = "deleteFilter", parameters = @ParamDef(name = "isDeleted", type = Boolean.class))
//@Filter(name = "deleteFilter", condition = "isDeleted = :isDeleted")
public abstract class BaseEntity {

    @Schema(hidden = true)
    @CreatedDate
    @Column(updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;

    @Schema(hidden = true)
    @LastModifiedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifiedDate;

    @Column(nullable = false)
    @Schema(hidden = true)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    @Schema(hidden = true)
    private LocalDateTime deletedAt;
    
}
