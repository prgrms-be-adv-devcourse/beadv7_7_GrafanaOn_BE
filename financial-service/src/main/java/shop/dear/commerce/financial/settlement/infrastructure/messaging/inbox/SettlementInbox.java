package shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import shop.dear.audit.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "settlement_inbox",
        uniqueConstraints = {
                // 같은 이벤트가 두 번 들어와도 한 건만 남긴다 (멱등성)
                @UniqueConstraint(
                        name = "uk_settlement_inbox_event_id",
                        columnNames = "event_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementInbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, updatable = false, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 50)
    private String eventType;

    @Column(length = 100, updatable = false)
    private String aggregateType;

    @Column(updatable = false)
    private Long aggregateId;

    @Column(nullable = false, updatable = false, length = 255)
    private String streamName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InboxMessageStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    private SettlementInbox(
            final String eventId,
            final String eventType,
            final String aggregateType,
            final Long aggregateId,
            final String streamName,
            final String payload,
            final InboxMessageStatus status,
            final String lastError
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.streamName = streamName;
        this.payload = payload;
        this.status = status;
        this.lastError = lastError;
        this.retryCount = 0;
    }

    public static SettlementInbox pending(
            final String eventId,
            final String eventType,
            final String aggregateType,
            final Long aggregateId,
            final String streamName,
            final String payload
    ) {
        return new SettlementInbox(
                eventId,
                eventType,
                aggregateType,
                aggregateId,
                streamName,
                payload,
                InboxMessageStatus.PENDING,
                null
        );
    }

    public static SettlementInbox failed(
            final String eventId,
            final String eventType,
            final String streamName,
            final String payload,
            final String lastError
    ) {
        return new SettlementInbox(
                eventId,
                eventType,
                null,
                null,
                streamName,
                payload,
                InboxMessageStatus.FAILED,
                lastError
        );
    }

    public boolean isPending() {
        return this.status == InboxMessageStatus.PENDING;
    }

    public void markAsProcessed() {
        this.status = InboxMessageStatus.PROCESSED;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed(final String reason) {
        this.retryCount++;
        this.lastError = reason;
        this.status = InboxMessageStatus.FAILED;
    }
}
