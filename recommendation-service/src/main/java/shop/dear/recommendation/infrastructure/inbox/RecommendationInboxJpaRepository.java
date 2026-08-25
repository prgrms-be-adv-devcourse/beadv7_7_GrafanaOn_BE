package shop.dear.recommendation.infrastructure.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecommendationInboxJpaRepository extends JpaRepository<RecommendationInbox, Long> {

    @Query("select i.eventId from RecommendationInbox i where i.eventId in :eventIds")
    List<Long> findEventIdsIn(@Param("eventIds") final List<Long> eventIds);
}
