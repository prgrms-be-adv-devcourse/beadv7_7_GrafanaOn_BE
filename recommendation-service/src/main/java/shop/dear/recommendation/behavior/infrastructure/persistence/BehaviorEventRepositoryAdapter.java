package shop.dear.recommendation.behavior.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.dear.recommendation.behavior.domain.model.RecommendationBehaviorEvent;
import shop.dear.recommendation.behavior.domain.repository.BehaviorEventRepository;
import shop.dear.recommendation.behavior.infrastructure.persistence.jpa.BehaviorEventJpaRepository;

@Repository
@RequiredArgsConstructor
public class BehaviorEventRepositoryAdapter implements BehaviorEventRepository {

    private final BehaviorEventJpaRepository behaviorEventJpaRepository;

    @Override
    public RecommendationBehaviorEvent save(final RecommendationBehaviorEvent event) {
        return behaviorEventJpaRepository.save(event);
    }

    @Override
    public boolean existsByEventId(String eventId) {
        return behaviorEventJpaRepository.existsByEventId(eventId);
    }
    
}
