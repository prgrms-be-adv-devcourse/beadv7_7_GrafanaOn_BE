package shop.dear.recommendation.behavior.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.recommendation.behavior.application.dto.TrackBehaviorCommand;
import shop.dear.recommendation.behavior.domain.model.RecommendationBehaviorEvent;
import shop.dear.recommendation.behavior.domain.repository.BehaviorEventRepository;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BehaviorEventService {

    private final BehaviorEventRepository behaviorEventRepository;

    @Transactional
    public void track(final TrackBehaviorCommand command) {
        if (behaviorEventRepository.existsByEventId(command.eventId())) {
            log.warn(
                    "[traceId={}] 중복된 행동 이벤트. eventId={}",
                    getTraceId(),
                    command.eventId()
            );
            return;
        }

        RecommendationBehaviorEvent event = RecommendationBehaviorEvent.create(
                command.eventId(),
                command.recommendationId(),
                command.memberId(),
                command.productId(),
                command.eventType(),
                command.occurredAt()
        );

        behaviorEventRepository.save(event);

        log.info(
                "[traceId={}] behavior_event_tracked "
                        + "eventId={} memberId={} productId={} "
                        + "eventType={} recommendationId={} occurredAt={}",
                getTraceId(),
                event.getEventId(),
                event.getMemberId(),
                event.getProductId(),
                event.getEventType(),
                event.getRecommendationId(),
                event.getOccurredAt()
        );
    }

    private String getTraceId() {
        return Optional.ofNullable(MDC.get("traceId"))
                .orElse("none");
    }
}