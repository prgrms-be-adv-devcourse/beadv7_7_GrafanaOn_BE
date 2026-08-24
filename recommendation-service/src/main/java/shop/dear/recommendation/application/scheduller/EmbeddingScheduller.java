package shop.dear.recommendation.application.scheduller;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import shop.dear.recommendation.application.dto.ProductPayload;
import shop.dear.recommendation.domain.constant.InboxStatus;
import shop.dear.recommendation.domain.model.RecommendationInbox;
import shop.dear.recommendation.domain.repository.RecommendationInboxRepository;
import shop.dear.recommendation.infrastructure.ai.ProductStoryEmbeddingService;

// payload 파싱 → 순서 역전 검사 → 재임베딩 필요 여부 → 임베딩 적재 → 상태 갱신
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "recommendation.pipeline.enabled", havingValue = "true")
public class EmbeddingScheduller {

	private final RecommendationInboxRepository inboxRepository;
	private final ProductStoryEmbeddingService embeddingService;
	private final ObjectMapper objectMapper;

	public void process(RecommendationInbox event) {
		ProductPayload payload = parse(event);
		if (payload == null) {
			markFailed(event, "payload 를 읽을 수 없습니다");
			return;
		}

		if (isStale(event)) {
			log.info("상품 {} 순서 역전 이벤트 건너뜀 (occurredAt={})", event.getAggregateId(), event.getOccurredAt());
			markProcessed(event);
			return;
		}

		String embeddingText = payload.story();

		if (this.embeddingService.isAlreadyEmbedded(payload.productId(), embeddingText)) {
			log.info("상품 {} Story 변경 없음 → 재임베딩 건너뜀", payload.productId());
			markProcessed(event);
			return;
		}

		try {
			this.embeddingService.embedAndStore(payload.productId(), embeddingText);
		} catch (Exception e) {
			log.error("상품 {} 임베딩 실패", payload.productId(), e);
			markFailed(event, e.getMessage());
			return;
		}

		markProcessed(event);
	}

	private ProductPayload parse(RecommendationInbox event) {
		try {
			ProductPayload payload = this.objectMapper.readValue(event.getPayload(), ProductPayload.class);
			return payload.isValid() ? payload : null;
		} catch (Exception e) {
			log.warn("Inbox {} payload 파싱 실패", event.getId(), e);
			return null;
		}
	}

	// 같은 상품에서 이미 처리한 이벤트보다 먼저 발생한 이벤트인지 확인
	private boolean isStale(RecommendationInbox event) {
		LocalDateTime lastProcessed = this.inboxRepository
			.findLatestOccurredAt(event.getAggregateId(), InboxStatus.PROCESSED)
			.orElse(null);
		return event.isStaleAgainst(lastProcessed);
	}

	private void markProcessed(RecommendationInbox event) {
		event.markAsProcessed();
		this.inboxRepository.save(event);
	}

	private void markFailed(RecommendationInbox event, String reason) {
		log.warn("Inbox {} 처리 실패: {}", event.getId(), reason);
		event.markAsFailed();
		this.inboxRepository.save(event);
	}
}
