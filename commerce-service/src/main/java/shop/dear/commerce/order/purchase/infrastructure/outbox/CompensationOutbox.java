package shop.dear.commerce.order.purchase.infrastructure.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.audit.BaseEntity;

import java.time.LocalDateTime;
import java.time.ZoneId;

// Order -> Product 보상 트랜잭션(Purchase 생성 실패 시 상품 상태 원복)을 위한 아웃박스.
// createPurchase 실패 시 Purchase 롤백과 별개의 트랜잭션(REQUIRES_NEW)으로 적재되어야 함 (CompensationOutboxWriter 참고)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "compensation_outbox")
@Entity
public class CompensationOutbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(name = "target_status", nullable = false, updatable = false, length = 30)
    private String targetStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CompensationOutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    private CompensationOutbox(final Long productId, final String targetStatus) {
        this.productId = productId;
        this.targetStatus = targetStatus;
        this.status = CompensationOutboxStatus.PENDING;
        this.retryCount = 0;
    }

    public static CompensationOutbox of(final Long productId, final String targetStatus) {
        return new CompensationOutbox(productId, targetStatus);
    }

    public void markSuccess() {
        this.status = CompensationOutboxStatus.SUCCESS;
        this.resolvedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public void markFailed(final String errorMessage) {
        this.retryCount++;
        this.lastError = errorMessage;
        this.status = CompensationOutboxStatus.FAILED;
    }
}
