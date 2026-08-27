package shop.dear.recommendation.infrastructure.inbox;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import shop.dear.recommendation.application.dto.ProductEvent;
import shop.dear.recommendation.application.port.ProductEventStore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RecommendationInboxAdapter implements ProductEventStore {

	private final RecommendationInboxJpaRepository jpaRepository;

	@Override
	public int append(final LocalDateTime now, final ProductEvent event) {
		return jpaRepository.insertIgnoringDuplicate(now,
			RecommendationInbox.of(
				event.eventId(),
				event.aggregateType(),
				event.aggregateId(),
				InboxEventType.valueOf(event.eventType()),
				event.payload(),
				event.occurredAt()
			)
		);
	}

	@Override
	public List<ProductEvent> findPending(final int limit) {
		return jpaRepository.findByStatusOrderByOccurredAtAsc(InboxStatus.PENDING, Limit.of(limit)).stream()
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

	private RecommendationInbox toInbox(final ProductEvent event) {
		return new RecommendationInbox();
	}
}
