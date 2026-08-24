package shop.dear.recommendation.domain.model;

import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
import shop.dear.recommendation.domain.constant.InboxEventType;
import shop.dear.recommendation.domain.constant.InboxStatus;

//Outbox 이벤트를 받아 적재하는 Inbox
@Entity
@Table(
	name = "recommendation_inbox",
	uniqueConstraints = {
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
public class RecommendationInbox {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_id", nullable = false, updatable = false)
	private Long eventId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, updatable = false, length = 50)
	private InboxEventType eventType;

	// 이벤트가 가리키는 대상 (productId)
	@Column(name = "aggregate_id", nullable = false, updatable = false)
	private Long aggregateId;

	// 이벤트 발생 시각
	@Column(name = "occurred_at", nullable = false, updatable = false)
	private LocalDateTime occurredAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "payload", nullable = false, updatable = false)
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private InboxStatus status;

	private RecommendationInbox(
		final Long eventId,
		final InboxEventType eventType,
		final Long aggregateId,
		final LocalDateTime occurredAt,
		final String payload
	) {
		this.eventId = eventId;
		this.eventType = eventType;
		this.aggregateId = aggregateId;
		this.occurredAt = occurredAt;
		this.payload = payload;
		this.status = InboxStatus.PENDING;
	}

	public static RecommendationInbox create(
		Long eventId,
		InboxEventType eventType,
		Long aggregateId,
		LocalDateTime occurredAt,
		String payload
	) {
		return new RecommendationInbox(eventId, eventType, aggregateId, occurredAt, payload);
	}

	public void markAsProcessed() {
		this.status = InboxStatus.PROCESSED;
	}

	public void markAsFailed() {
		this.status = InboxStatus.FAILED;
	}

	public boolean isPending() {
		return this.status == InboxStatus.PENDING;
	}

	// 과거 이벤트로 최신 데이터를 덮어쓰지 않기 위해 검증
	public boolean isStaleAgainst(LocalDateTime lastProcessedOccurredAt) {
		return lastProcessedOccurredAt != null && this.occurredAt.isBefore(lastProcessedOccurredAt);
	}
}
