package shop.dear.audit;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public class BaseEntity {

    @Column(name = "inserted_at", nullable = false, updatable = false)
    private LocalDateTime insertedAt;

    @Column(name = "updated_at", nullable = false, updatable = true)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.insertedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
