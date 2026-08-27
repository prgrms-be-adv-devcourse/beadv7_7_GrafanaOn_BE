package shop.dear.recommendation.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import shop.dear.recommendation.domain.model.Embedding;
import shop.dear.recommendation.domain.repository.ProductEmbedder;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "recommendation.pipeline", name = "enabled", havingValue = "true")
public class SpringAiProductEmbedder implements ProductEmbedder {

	// EmbeddingModel : 텍스트를 유사도 계산과 벡터로 변환해주는 추상화 인터페이스
	private final EmbeddingModel embeddingModel;
	private final String modelName;

	// model_name은 로컬과 운영이 다르므로 환경변수로 주입
	public SpringAiProductEmbedder(
		final EmbeddingModel embeddingModel,
		@Value("${recommendation.embedding.model-name}") final String modelName
	)
	{
		this.embeddingModel = embeddingModel;
		this.modelName = modelName;
	}

	@Override
	public Embedding embed(final String story) {

		return new Embedding(this.modelName, this.embeddingModel.embed(story));
	}
}
