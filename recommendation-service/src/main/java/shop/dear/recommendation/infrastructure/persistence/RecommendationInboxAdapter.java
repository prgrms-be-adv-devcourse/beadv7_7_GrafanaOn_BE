package shop.dear.recommendation.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import shop.dear.recommendation.domain.constant.InboxStatus;
import shop.dear.recommendation.domain.model.RecommendationInbox;
import shop.dear.recommendation.domain.repository.RecommendationInboxRepository;
import shop.dear.recommendation.infrastructure.persistence.jpa.RecommendationInboxJpaRepository;


@Repository
@RequiredArgsConstructor
public class RecommendationInboxAdapter implements RecommendationInboxRepository {

	private final RecommendationInboxJpaRepository jpaRepository;

	@Override
	public RecommendationInbox save(RecommendationInbox inbox) {
		return this.jpaRepository.save(inbox);
	}

	@Override
	public boolean existsByEventId(Long eventId) {
		// PK(id) 가 아니라 발행 측이 부여한 event_id 로 찾는다.
		return this.jpaRepository.existsByEventId(eventId);
	}

	@Override
	public List<RecommendationInbox> findByStatusOrderByOccurredAtAsc(InboxStatus status, int limit) {
		return this.jpaRepository.findByStatusOrderByOccurredAtAsc(status, PageRequest.of(0, limit));
	}

	@Override
	public Optional<LocalDateTime> findLatestOccurredAt(Long aggregateId, InboxStatus status) {
		return this.jpaRepository.findLatestOccurredAt(aggregateId, status);
	}
}
