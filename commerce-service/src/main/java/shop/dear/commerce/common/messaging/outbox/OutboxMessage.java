package shop.dear.commerce.common.messaging.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.audit.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "outbox_message")
public class OutboxMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(length = 100)
    private String aggregateType;

    private Long aggregateId;

    @Column(nullable = false, length = 255)
    private String streamName;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxMessageStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    private OutboxMessage(
            final String eventId,
            final String eventType,
            final String aggregateType,
            final Long aggregateId,
            final String streamName,
            final String payload
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.streamName = streamName;
        this.payload = payload;
        this.status = OutboxMessageStatus.PENDING;
        this.retryCount = 0;
    }

    public static OutboxMessage create(
            final String eventType,
            final String aggregateType,
            final Long aggregateId,
            final String streamName,
            final String payload
    ) {
        return new OutboxMessage(
                UUID.randomUUID().toString(),
                eventType,
                aggregateType,
                aggregateId,
                streamName,
                payload
        );
    }

    public void markPublished() {
        this.status = OutboxMessageStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void fail(final String errorMessage) {
        this.retryCount++;
        this.errorMessage = errorMessage;
    }

    public void markDlq() {
        this.status = OutboxMessageStatus.DLQ;
    }
}
