package shop.dear.recommendation.infrastructure.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface RecommendationInboxJpaRepository extends JpaRepository<RecommendationInbox, Long> {

    // 중복 판정과 적재를 한 문장에 묶어 DB가 중복을 조용히 버리게 하여 멱등성을 보장합니다.
    // JPQL에는 INSERT 문이 없어 네이티브 쿼리로 작성하며, {h-schema}는 Hibernate가 default_schema로 치환한다.
    @Modifying
    @Query(value = """
        insert into {h-schema}recommendation_inbox (
            inserted_at, updated_at, event_id, aggregate_type, aggregate_id,
            event_type, payload, status, retry_count, occurred_at
        ) values (
            :now, :now, :#{#inbox.eventId}, :#{#inbox.aggregateType}, :#{#inbox.aggregateId},
            :#{#inbox.eventType.name()}, cast(:#{#inbox.payload} as jsonb), :#{#inbox.status.name()},
            :#{#inbox.retryCount}, :#{#inbox.occurredAt}
        )
        on conflict (event_id) do nothing
        """, nativeQuery = true)
    int insertIgnoringDuplicate(
        @Param("now") final LocalDateTime now,
        @Param("inbox") final RecommendationInbox inbox
    );
}
