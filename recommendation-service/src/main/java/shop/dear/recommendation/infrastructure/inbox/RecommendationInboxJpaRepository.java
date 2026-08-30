package shop.dear.recommendation.infrastructure.inbox;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecommendationInboxJpaRepository extends JpaRepository<RecommendationInbox, Long> {

    List<RecommendationInbox> findByStatusInAndRetryCountLessThanOrderByOccurredAtAsc(
        final Collection<InboxStatus> statuses,
        final int retryCountLessThan,
        final Limit limit
    );

    Optional<RecommendationInbox> findByEventId(final Long eventId);

    @Query("""
        select max(i.occurredAt)
        from RecommendationInbox i
        where i.aggregateId = :aggregateId
          and i.status = :status
        """)
    Optional<LocalDateTime> findLatestOccurredAt(
        @Param("aggregateId") final String aggregateId,
        @Param("status") final InboxStatus status
    );
}
