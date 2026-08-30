package shop.dear.recommendation.behavior.domain.repository;

import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.recommendation.behavior.domain.model.RecommendationItem;

import java.util.List;

public interface RecommendationItemRepository {
    List<RecommendationItem> findCandidates(List<ProductCategory> categories);
    List<RecommendationItem> findAllByProductIdIn(List<Long> productIds);
}
