package shop.dear.recommendation.behavior.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.recommendation.behavior.domain.model.RecommendationBehaviorEvent;

import java.time.LocalDateTime;
import java.util.List;

public interface BehaviorEventJpaRepository extends JpaRepository<RecommendationBehaviorEvent, Long> {
    boolean existsByEventId(String eventId);
    List<RecommendationBehaviorEvent> findByMemberIdAndOccurredAtAfter(Long memberId, LocalDateTime occurredAt);
}
