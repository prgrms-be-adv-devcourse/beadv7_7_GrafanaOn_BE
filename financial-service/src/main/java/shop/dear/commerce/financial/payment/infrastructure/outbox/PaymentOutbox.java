package shop.dear.commerce.financial.payment.infrastructure.outbox;

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
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "payment_outbox")
public class PaymentOutbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "event_id",
            nullable = false,
            updatable = false,
            unique = true,
            length = 36
    )
    private String eventId;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false)
    private PaymentOutboxEventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentOutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    private PaymentOutbox(
            final Long paymentId,
            final PaymentOutboxEventType eventType,
            final String payload
    ) {
        this.eventId = UUID.randomUUID().toString();
        this.paymentId = paymentId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = PaymentOutboxStatus.PENDING;
        this.retryCount = 0;
    }

    public static PaymentOutbox of(
            final Long paymentId,
            final PaymentOutboxEventType eventType,
            final String payload
    ) {
        return new PaymentOutbox(paymentId, eventType, payload);
    }

    public void markSent() {
        this.status = PaymentOutboxStatus.SENT;
        this.sentAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public void markFailed(final String errorMessage) {
        this.status = PaymentOutboxStatus.FAILED;
        this.retryCount++;
        this.lastError = errorMessage;
    }

    public void markExhausted() {
        this.status = PaymentOutboxStatus.EXHAUSTED;
    }
}
