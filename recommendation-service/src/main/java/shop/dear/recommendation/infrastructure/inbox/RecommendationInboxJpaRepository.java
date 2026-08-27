package shop.dear.recommendation.infrastructure.inbox;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationInboxJpaRepository extends JpaRepository<RecommendationInbox, Long> {
}
