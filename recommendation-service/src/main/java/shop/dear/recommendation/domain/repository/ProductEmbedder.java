package shop.dear.recommendation.domain.repository;

import shop.dear.recommendation.domain.model.Embedding;

public interface ProductEmbedder {

	Embedding embed(String story);
}
