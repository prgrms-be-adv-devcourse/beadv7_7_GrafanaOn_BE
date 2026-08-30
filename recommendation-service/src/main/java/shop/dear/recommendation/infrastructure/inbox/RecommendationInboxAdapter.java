package shop.dear.recommendation.infrastructure.inbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import shop.dear.recommendation.application.dto.ProductEvent;
import shop.dear.recommendation.application.port.ProductEventStore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class RecommendationInboxAdapter implements ProductEventStore {

	private final int MAX_RETRY_COUNT;

	private final List<InboxStatus> PROCESSABLE = List.of(InboxStatus.PENDING, InboxStatus.FAILED);

	private final RecommendationInboxJpaRepository jpaRepository;

	// 재시도 한도를 넘긴 FAILED 는 조회에서 제외
	public RecommendationInboxAdapter(
		final RecommendationInboxJpaRepository jpaRepository,
		@Value("${recommendation.inbox.max-retry-count}") final int maxRetryCount
	) {
		this.jpaRepository = jpaRepository;
		this.MAX_RETRY_COUNT = maxRetryCount;
	}

	@Override
	public List<ProductEvent> findProcessable(final int limit) {
		return jpaRepository.findByStatusInAndRetryCountLessThanOrderByOccurredAtAsc(
				PROCESSABLE,
				MAX_RETRY_COUNT,
				Limit.of(limit)
			).stream()
			.map(this::toEvent)
			.toList();
	}

	@Override
	public Optional<LocalDateTime> findLatestProcessedOccurredAt(final String aggregateId) {
		return jpaRepository.findLatestOccurredAt(aggregateId, InboxStatus.PROCESSED);
	}

	@Override
	public void markProcessed(final Long eventId) {
		jpaRepository.findByEventId(eventId).ifPresent(RecommendationInbox::markAsProcessed);
	}

	@Override
	public void markFailed(final Long eventId, final String reason) {
		jpaRepository.findByEventId(eventId).ifPresent(inbox -> inbox.markAsFailed(reason));
	}

	private ProductEvent toEvent(final RecommendationInbox inbox) {
		return new ProductEvent(
			inbox.getEventId(),
			inbox.getAggregateType(),
			inbox.getAggregateId(),
			inbox.getEventType().name(),
			inbox.getPayload(),
			inbox.getOccurredAt()
		);
	}
}
