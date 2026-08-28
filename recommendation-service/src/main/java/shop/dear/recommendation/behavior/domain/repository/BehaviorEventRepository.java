package shop.dear.recommendation.behavior.domain.repository;

import shop.dear.recommendation.behavior.domain.model.RecommendationBehaviorEvent;

public interface BehaviorEventRepository {
    RecommendationBehaviorEvent save(RecommendationBehaviorEvent event);

    boolean existsByEventId(String eventId);
}
