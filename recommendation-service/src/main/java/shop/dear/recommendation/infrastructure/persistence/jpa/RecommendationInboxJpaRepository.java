package shop.dear.recommendation.infrastructure.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import shop.dear.recommendation.domain.constant.InboxStatus;
import shop.dear.recommendation.domain.model.RecommendationInbox;

public interface RecommendationInboxJpaRepository extends JpaRepository<RecommendationInbox, Long> {

	boolean existsByEventId(Long eventId);

	List<RecommendationInbox> findByStatusOrderByOccurredAtAsc(InboxStatus status, Pageable pageable);

	@Query("""
		SELECT MAX(i.occurredAt) FROM RecommendationInbox i
		WHERE i.aggregateId = :aggregateId
		  AND i.status = :status
		""")
	Optional<LocalDateTime> findLatestOccurredAt(
		@Param("aggregateId") Long aggregateId,
		@Param("status") InboxStatus status
	);
}
