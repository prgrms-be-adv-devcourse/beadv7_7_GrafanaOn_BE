package shop.dear.recommendation.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import shop.dear.recommendation.domain.constant.InboxStatus;
import shop.dear.recommendation.domain.model.RecommendationInbox;

public interface RecommendationInboxRepository {

	RecommendationInbox save(RecommendationInbox inbox);

	boolean existsByEventId(Long eventId);

	List<RecommendationInbox> findByStatusOrderByOccurredAtAsc(InboxStatus status, int limit);

	Optional<LocalDateTime> findLatestOccurredAt(Long aggregateId, InboxStatus status);
}
