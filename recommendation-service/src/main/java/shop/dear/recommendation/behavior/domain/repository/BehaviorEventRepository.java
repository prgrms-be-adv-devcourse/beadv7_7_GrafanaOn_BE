package shop.dear.recommendation.behavior.domain.repository;

import shop.dear.recommendation.behavior.domain.model.RecommendationBehaviorEvent;

import java.time.LocalDateTime;
import java.util.List;

public interface BehaviorEventRepository {
    RecommendationBehaviorEvent save(RecommendationBehaviorEvent event);

    boolean existsByEventId(String eventId);

    List<RecommendationBehaviorEvent> findByMemberIdAndOccurredAtAfter(Long memberId, LocalDateTime occurredAt);
}
