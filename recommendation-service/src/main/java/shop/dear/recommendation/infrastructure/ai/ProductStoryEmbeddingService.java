package shop.dear.recommendation.infrastructure.ai;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import shop.dear.recommendation.infrastructure.ai.db.ProductVectorRepository;

// 상품 Story 를 임베딩하여 product_vector에 적재
@Slf4j
@Service
@ConditionalOnProperty(name = "recommendation.pipeline.enabled", havingValue = "true")
public class ProductStoryEmbeddingService {

	private final EmbeddingModel embeddingModel;
	private final ProductVectorRepository productVectorRepository;
	private final String modelName;

	public ProductStoryEmbeddingService(
		EmbeddingModel embeddingModel,
		ProductVectorRepository productVectorRepository,
		@Value("${recommendation.embedding.model-name}") String modelName
	) {
		this.embeddingModel = embeddingModel;
		this.productVectorRepository = productVectorRepository;
		this.modelName = modelName;
	}

	public void embedAndStore(Long productId, String story) {
		long startedAt = System.currentTimeMillis();
		float[] embedding = this.embeddingModel.embed(story);
		long embeddedAt = System.currentTimeMillis();

		this.productVectorRepository.upsert(productId, story, this.modelName, embedding);

		log.info("상품 {} 임베딩 적재 완료 (임베딩 {}ms, 저장 {}ms, {}차원)",
			productId, embeddedAt - startedAt, System.currentTimeMillis() - embeddedAt, embedding.length);
	}

	public boolean isAlreadyEmbedded(Long productId, String story) {
		return this.productVectorRepository.findStory(productId, this.modelName)
			.filter(story::equals)
			.isPresent();
	}
}
