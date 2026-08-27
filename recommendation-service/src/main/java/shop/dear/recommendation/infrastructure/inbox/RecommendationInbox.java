package shop.dear.recommendation.infrastructure.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import shop.dear.audit.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "recommendation_inbox",
    uniqueConstraints = {
        // 같은 이벤트가 두 번 들어와도 한 건만 남긴다 (멱등성)
        @UniqueConstraint(
            name = "uk_recommendation_inbox_event_id",
            columnNames = "event_id"
        )
    },
    indexes = {
        // PENDING 인 것을 발생 순서대로 꺼내는 조회
        @Index(
            name = "idx_recommendation_inbox_status_occurred_at",
            columnList = "status, occurred_at"
        ),
        // 특정 상품의 마지막 처리 시각을 확인하는 조회
        @Index(
            name = "idx_recommendation_inbox_aggregate_occurred_at",
            columnList = "aggregate_id, occurred_at"
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationInbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    // 발신 측 outbox 행의 id. 중복 적재를 걸러내는 멱등키다.
    @Column(name = "event_id", nullable = false, updatable = false)
    private Long eventId;

    @Column(name = "aggregate_type", nullable = false, updatable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 50)
    private InboxEventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    // 이벤트가 발신 측에서 발생한 시각
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    private RecommendationInbox(
        final Long eventId,
        final String aggregateType,
        final String aggregateId,
        final InboxEventType eventType,
        final String payload,
        final LocalDateTime occurredAt
    ) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.status = InboxStatus.PENDING;
        this.retryCount = 0;
    }

    public static RecommendationInbox of(
        final Long eventId,
        final String aggregateType,
        final String aggregateId,
        final InboxEventType eventType,
        final String payload,
        final LocalDateTime occurredAt
    ) {
        return new RecommendationInbox(eventId, aggregateType, aggregateId, eventType, payload, occurredAt);
    }

    public void markAsProcessed() {
        this.status = InboxStatus.PROCESSED;
    }

    public void markAsFailed(final String reason) {
        this.retryCount++;
        this.lastError = reason;
        this.status = InboxStatus.FAILED;
    }

    public boolean isPending() {
        return this.status == InboxStatus.PENDING;
    }
}
