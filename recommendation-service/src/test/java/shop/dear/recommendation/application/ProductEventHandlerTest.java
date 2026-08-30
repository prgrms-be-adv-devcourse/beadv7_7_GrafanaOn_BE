package shop.dear.recommendation.application;

import shop.dear.recommendation.application.handler.ProductEventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.recommendation.domain.model.Embedding;
import shop.dear.recommendation.domain.repository.ProductVectorRepository;
import shop.dear.recommendation.application.port.ProductEmbedder;
import shop.dear.recommendation.application.dto.ProductEvent;
import tools.jackson.databind.json.JsonMapper;
import shop.dear.recommendation.application.port.ProductEventStore;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ProductEventHandlerTest {

	private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 8, 25, 12, 0, 0);
	private static final Long PRODUCT_ID = 100L;
	private static final String MODEL_NAME = "bge-m3";

	@Mock
	private ProductEventStore eventStore;

	@Mock
	private ProductEmbedder productEmbedder;

	@Mock
	private ProductVectorRepository productVectorRepository;

	@Mock
	private EmbeddingService embeddingService;

	private ProductEventHandler productEventHandler;

	@BeforeEach
	void setUp() {
		productEventHandler = new ProductEventHandler(
			eventStore, productVectorRepository, productEmbedder,
			embeddingService, JsonMapper.builder().build()
		);
	}

	private ProductEvent event(final String eventType, final String payload) {
		return new ProductEvent(1L, "Product", String.valueOf(PRODUCT_ID), eventType, payload, OCCURRED_AT);
	}

	private void neverProcessedBefore() {
		given(eventStore.findLatestProcessedOccurredAt(String.valueOf(PRODUCT_ID)))
			.willReturn(Optional.empty());
	}

	@Test
	@DisplayName("삭제 이벤트는 임베딩하지 않고 벡터를 지운다")
	void deleteEventRemovesVector() {
		neverProcessedBefore();
		final ProductEvent event =
			event("PRODUCT_DELETED", "{\"productId\":100,\"story\":null}");

		productEventHandler.process(event);

		then(embeddingService).should().delete(event.eventId(), PRODUCT_ID);
		then(productEmbedder).should(never()).embed(anyString());
		then(embeddingService).should(never()).save(anyLong(), anyLong(), anyString(), any());
	}

	@Test
	@DisplayName("삭제 이벤트라도 이미 처리한 이벤트보다 오래됐으면 벡터를 지우지 않는다")
	void staleDeleteEventIsSkipped() {
		given(eventStore.findLatestProcessedOccurredAt(String.valueOf(PRODUCT_ID)))
			.willReturn(Optional.of(OCCURRED_AT.plusSeconds(1)));
		final ProductEvent event =
			event("PRODUCT_DELETED", "{\"productId\":100,\"story\":null}");

		productEventHandler.process(event);

		then(embeddingService).should().markProcessed(event.eventId());
		then(embeddingService).should(never()).delete(anyLong(), anyLong());
	}

	@Test
	@DisplayName("갱신 이벤트인데 story가 비어 있으면 실패로 남긴다")
	void updateEventWithoutStoryFails() {
		neverProcessedBefore();
		final ProductEvent event =
			event("PRODUCT_UPDATED", "{\"productId\":100,\"story\":null}");

		productEventHandler.process(event);

		then(embeddingService).should().markFailed(eq(event.eventId()), anyString());
		then(productEmbedder).should(never()).embed(anyString());
	}

	@Test
	@DisplayName("갱신 이벤트는 임베딩 후 벡터를 적재한다")
	void updateEventEmbedsStory() {
		neverProcessedBefore();
		final Embedding embedding = new Embedding(MODEL_NAME, new float[] {0.1f, 0.2f});
		// 저장된 원문이 없다 = 아직 임베딩 전
		given(productVectorRepository.findStory(PRODUCT_ID)).willReturn(Optional.empty());
		given(productEmbedder.embed("이야기")).willReturn(embedding);
		final ProductEvent event =
			event("PRODUCT_UPDATED", "{\"productId\":100,\"story\":\"이야기\"}");

		productEventHandler.process(event);

		// 벡터와 모델명은 Embedding 하나로 함께 넘어간다. Embedding 은 내용으로 비교되므로 그대로 단언한다
		then(embeddingService).should().save(event.eventId(), PRODUCT_ID, "이야기", embedding);
		then(embeddingService).should(never()).delete(anyLong(), anyLong());
	}

	// 파이프라인에서 가장 비싼 구간을 건너뛰는 분기다
	@Test
	@DisplayName("저장된 story가 새 story와 같으면 재임베딩하지 않는다")
	void unchangedStorySkipsEmbedding() {
		neverProcessedBefore();
		given(productVectorRepository.findStory(PRODUCT_ID))
			.willReturn(Optional.of("이야기"));
		final ProductEvent event =
			event("PRODUCT_UPDATED", "{\"productId\":100,\"story\":\"이야기\"}");

		productEventHandler.process(event);

		then(productEmbedder).should(never()).embed(anyString());
		then(embeddingService).should().markProcessed(event.eventId());
		then(embeddingService).should(never()).save(anyLong(), anyLong(), anyString(), any());
	}

	@Test
	@DisplayName("payload를 읽을 수 없으면 실패로 남긴다")
	void unreadablePayloadFails() {
		final ProductEvent event = event("PRODUCT_UPDATED", "깨진 payload");

		productEventHandler.process(event);

		then(embeddingService).should().markFailed(eq(event.eventId()), anyString());
		then(productEmbedder).should(never()).embed(anyString());
	}

	@Test
	@DisplayName("임베딩 호출이 실패하면 적재하지 않고 실패로 남긴다")
	void embeddingFailureIsMarkedFailed() {
		neverProcessedBefore();
		final ProductEvent event =
			event("PRODUCT_UPDATED", "{\"productId\":100,\"story\":\"이야기\"}");
		given(productVectorRepository.findStory(PRODUCT_ID)).willReturn(Optional.empty());
		given(productEmbedder.embed(anyString())).willThrow(new RuntimeException("모델 서버 연결 실패"));

		productEventHandler.process(event);

		then(embeddingService).should().markFailed(eq(event.eventId()), anyString());
		then(embeddingService).should(never()).save(anyLong(), anyLong(), anyString(), any());
	}
}
