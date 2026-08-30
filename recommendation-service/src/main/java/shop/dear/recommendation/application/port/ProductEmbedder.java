package shop.dear.recommendation.application.port;

import shop.dear.recommendation.domain.model.Embedding;

public interface ProductEmbedder {

	Embedding embed(String story);
}
