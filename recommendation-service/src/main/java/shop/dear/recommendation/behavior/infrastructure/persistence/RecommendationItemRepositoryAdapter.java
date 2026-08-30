package shop.dear.recommendation.behavior.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.recommendation.behavior.domain.model.RecommendationItem;
import shop.dear.recommendation.behavior.domain.repository.RecommendationItemRepository;
import shop.dear.recommendation.behavior.infrastructure.persistence.jpa.RecommendationItemJpaRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RecommendationItemRepositoryAdapter implements RecommendationItemRepository {

    private final RecommendationItemJpaRepository recommendationItemJpaRepository;

    @Override
    public List<RecommendationItem> findCandidates(final List<ProductCategory> categories) {
        return recommendationItemJpaRepository.findByCategoryInAndStatus(categories, ProductStatus.ON_SALE);
    }

    @Override
    public List<RecommendationItem> findAllByProductIdIn(final List<Long> productIds) {
        return recommendationItemJpaRepository.findAllByProductIdIn(productIds);
    }
}
