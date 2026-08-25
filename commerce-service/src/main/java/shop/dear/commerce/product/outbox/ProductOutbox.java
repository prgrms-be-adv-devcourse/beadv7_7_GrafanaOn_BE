package shop.dear.commerce.product.outbox;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import shop.dear.audit.BaseEntity;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product_outbox")
@Entity
public class ProductOutbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, updatable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false)
    private ProductOutboxEvent eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductOutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    private ProductOutbox(
        final String aggregateType,
        final String aggregateId,
        final ProductOutboxEvent eventType,
        final String payload
    ) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = ProductOutboxStatus.PENDING;
        this.retryCount = 0;
    }

    public static ProductOutbox of(
        final String aggregateType,
        final String aggregateId,
        final ProductOutboxEvent eventType,
        final String payload
    ) {
        return new ProductOutbox(aggregateType, aggregateId, eventType, payload);
    }

    public void markSent() {
        this.status = ProductOutboxStatus.SENT;
        this.sentAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public void markFailed(final String errorMessage) {
        this.retryCount++;
        this.lastError = errorMessage;
        this.status = ProductOutboxStatus.FAILED;
    }
}
