package shop.dear.recommendation.application.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import shop.dear.recommendation.application.EmbeddingService;
import shop.dear.recommendation.application.dto.ProductEvent;
import shop.dear.recommendation.application.dto.ProductPayload;
import shop.dear.recommendation.application.port.ProductEventStore;
import shop.dear.recommendation.application.port.ProductEmbedder;
import shop.dear.recommendation.domain.model.Embedding;
import shop.dear.recommendation.domain.repository.ProductVectorRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

// payload 파싱 → 순서 역전 검사 → 이벤트 종류에 따라 삭제 또는 임베딩
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventHandler {

	private final ProductEventStore eventStore;
	private final ProductVectorRepository productVectorRepository;
	private final ProductEmbedder productEmbedder;
	private final EmbeddingService embeddingService;
	private final ObjectMapper objectMapper; //json ↔ java 직렬화/역직렬화

	public void process(ProductEvent event) {

		ProductPayload payload = parse(event);

		if (payload == null || !payload.hasProductId()) {
			this.embeddingService.markFailed(event.eventId(), "payload 에서 productId 를 읽을 수 없습니다");
			return;
		}

		if (isStale(event)) {
			log.info("상품 {} 순서 역전 이벤트 건너뜀 (occurredAt={})", event.aggregateId(), event.occurredAt());
			this.embeddingService.markProcessed(event.eventId());
			return;
		}

		if (event.isDeleted()) {
			deleteVector(event, payload);
			return;
		}

		embedStory(event, payload);
	}

	private void deleteVector(ProductEvent event, ProductPayload payload) {
		try {
			this.embeddingService.delete(event.eventId(), payload.productId());
		} catch (Exception e) {
			log.error("상품 {} 벡터 삭제 실패", payload.productId(), e);
			this.embeddingService.markFailed(event.eventId(), e.getMessage());
		}
	}

	private void embedStory(ProductEvent event, ProductPayload payload) {
		if (!payload.hasStory()) {
			this.embeddingService.markFailed(event.eventId(), "임베딩할 story 가 비어 있습니다");
			return;
		}

		String embeddingText = payload.story();

		if (isAlreadyEmbedded(payload.productId(), embeddingText)) {
			log.info("상품 {} Story 변경이 없어 임베딩 건너뜀", payload.productId());
			this.embeddingService.markProcessed(event.eventId());
			return;
		}

		Embedding embedding;
		try {
			embedding = this.productEmbedder.embed(embeddingText);
		} catch (Exception e) {
			log.error("상품 {} 임베딩 실패", payload.productId(), e);
			this.embeddingService.markFailed(event.eventId(), e.getMessage());
			return;
		}

		try {
			this.embeddingService.save(event.eventId(), payload.productId(), embeddingText, embedding);
		} catch (Exception e) {
			log.error("상품 {} 벡터 적재 실패", payload.productId(), e);
			this.embeddingService.markFailed(event.eventId(), e.getMessage());
		}
	}

	private boolean isAlreadyEmbedded(final Long productId, final String story) {
		return this.productVectorRepository.findStory(productId)
			.filter(story::equals)
			.isPresent();
	}

	private ProductPayload parse(ProductEvent event) {
		try {
			//json 문자열을 역직렬화
			return this.objectMapper.readValue(event.payload(), ProductPayload.class);
		} catch (Exception e) {
			log.warn("이벤트 {} payload 파싱 실패", event.eventId(), e);
			return null;
		}
	}

	private boolean isStale(ProductEvent event) {
		LocalDateTime lastProcessed = this.eventStore
			.findLatestProcessedOccurredAt(event.aggregateId())
			.orElse(null);
		return event.isStaleAgainst(lastProcessed);
	}
}
