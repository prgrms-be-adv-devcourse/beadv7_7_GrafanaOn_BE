package shop.dear.recommendation.behavior.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.recommendation.behavior.domain.model.RecommendationBehaviorEvent;

public interface BehaviorEventJpaRepository extends JpaRepository<RecommendationBehaviorEvent, Long> {
    boolean existsByEventId(String eventId);
}
