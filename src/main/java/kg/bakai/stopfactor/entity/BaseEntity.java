package kg.bakai.stopfactor.entity;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
public class BaseEntity {

    protected LocalDateTime createdDate;
    protected LocalDateTime updatedDate;

    @PrePersist
    public void beforePersist() {
        this.createdDate = LocalDateTime.now();
    }

    @PreUpdate
    public void beforeUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}