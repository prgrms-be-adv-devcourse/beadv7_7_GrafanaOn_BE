package shop.dear.commerce.product.infrastructure.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import shop.dear.audit.BaseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "product_inbox",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_product_inbox_consumer_event",
        columnNames = {"consumer_name", "event_id"}
    )
)
@Entity
public class ProductInbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer_name", nullable = false, updatable = false)
    private String consumerName;

    @Column(name = "event_id", nullable = false, updatable = false, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 50)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    private ProductInbox(
        final String consumerName,
        final String eventId,
        final String eventType,
        final String payload,
        final InboxStatus status,
        final String lastError
    ) {
        this.consumerName = consumerName;
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.retryCount = 0;
        this.lastError = lastError;
    }

    public static ProductInbox pending(
        final String consumerName,
        final String eventId,
        final String eventType,
        final String payload
    ) {
        return new ProductInbox(
            consumerName,
            eventId,
            eventType,
            payload,
            InboxStatus.PENDING,
            null
        );
    }

    public void markAsProcessed() {
        this.status = InboxStatus.PROCESSED;
    }

    public void markAsFailed(final String reason) {
        this.retryCount++;
        this.lastError = reason;
        this.status = InboxStatus.FAILED;
    }
}
