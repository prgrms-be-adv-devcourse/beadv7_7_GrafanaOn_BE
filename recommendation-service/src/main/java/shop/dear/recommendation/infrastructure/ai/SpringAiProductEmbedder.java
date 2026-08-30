package shop.dear.recommendation.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import shop.dear.recommendation.domain.model.Embedding;
import shop.dear.recommendation.application.port.ProductEmbedder;

import java.util.List;

@Slf4j
@Component
public class SpringAiProductEmbedder implements ProductEmbedder {

	// EmbeddingModel : 텍스트를 유사도 계산과 벡터로 변환해주는 추상화 인터페이스
	private final EmbeddingModel embeddingModel;
	private final String modelName;
	private final DocumentNormalizer documentNormalizer;

	// model_name은 로컬과 운영이 다르므로 환경변수로 주입
	public SpringAiProductEmbedder(
		final EmbeddingModel embeddingModel,
		@Value("${recommendation.embedding.model-name}") final String modelName,
		final DocumentNormalizer documentNormalizer
	)
	{
		this.embeddingModel = embeddingModel;
		this.modelName = modelName;
		this.documentNormalizer = documentNormalizer;
	}

	@Override
	public Embedding embed(final String story) {

		//STORY 정규화 필요
		final String normalized = documentNormalizer
			.apply(List.of(new Document(story)))
			.getFirst()
			.getText();

		if (normalized != null) {
			return new Embedding(this.modelName, this.embeddingModel.embed(normalized));
		} else {
			return  null;
		}
	}
}
