package shop.dear.recommendation.behavior.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.audit.BaseEntity;
import shop.dear.common.exception.BusinessException;
import shop.dear.recommendation.behavior.domain.constant.BehaviorType;
import shop.dear.recommendation.behavior.domain.exception.BehaviorErrorCode;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "recommendation_behavior_event",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_behavior_event_event_id",
                        columnNames = "event_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationBehaviorEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "recommendation_id")
    private String recommendationId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private BehaviorType eventType;

    // 실제 행동(이벤트) 발생 시간
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    private RecommendationBehaviorEvent(
            final String eventId,
            final String recommendationId,
            final Long memberId,
            final Long productId,
            final BehaviorType eventType,
            final LocalDateTime occurredAt
    ) {
        validateRecommendationId(eventType, recommendationId);

        this.eventId = eventId;
        this.recommendationId = recommendationId;
        this.memberId = memberId;
        this.productId = productId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
    }

    public static RecommendationBehaviorEvent create(
            final String eventId,
            final String recommendationId,
            final Long memberId,
            final Long productId,
            final BehaviorType eventType,
            final LocalDateTime occurredAt
    ) {
        return new RecommendationBehaviorEvent(
                eventId,
                recommendationId,
                memberId,
                productId,
                eventType,
                occurredAt
        );
    }

    //RecommendationID 검증
    private static void validateRecommendationId(
            final BehaviorType eventType,
            final String recommendationId
    ) {
        if((eventType == BehaviorType.IMPRESSION || eventType == BehaviorType.CLICK) && recommendationId == null) {
            throw new BusinessException(BehaviorErrorCode.RECOMMENDATION_ID_REQUIRED);
        }
    }
}