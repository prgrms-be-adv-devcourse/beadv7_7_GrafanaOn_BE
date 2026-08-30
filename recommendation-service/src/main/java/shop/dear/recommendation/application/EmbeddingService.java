package shop.dear.recommendation.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import shop.dear.recommendation.application.port.ProductEventStore;
import shop.dear.recommendation.domain.model.Embedding;
import shop.dear.recommendation.domain.repository.ProductVectorRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

	private final ProductEventStore eventStore;
	private final ProductVectorRepository productVectorRepository;

	@Transactional
	public void save(
		final Long eventId,
		final Long productId,
		final String story,
		final Embedding embedding
	) {
		this.productVectorRepository.save(productId, story, embedding);

		this.eventStore.markProcessed(eventId);
	}

	@Transactional
	public void delete(final Long eventId, final Long productId) {
		final int removed = this.productVectorRepository.deleteByProductId(productId);
		log.info("상품 {} 벡터 삭제 ({}건)", productId, removed);

		this.eventStore.markProcessed(eventId);
	}

	@Transactional
	public void markProcessed(final Long eventId) {
		this.eventStore.markProcessed(eventId);
	}

	@Transactional
	public void markFailed(final Long eventId, final String reason) {
		log.warn("이벤트 {} 처리 실패: {}", eventId, reason);

		this.eventStore.markFailed(eventId, reason);
	}
}
