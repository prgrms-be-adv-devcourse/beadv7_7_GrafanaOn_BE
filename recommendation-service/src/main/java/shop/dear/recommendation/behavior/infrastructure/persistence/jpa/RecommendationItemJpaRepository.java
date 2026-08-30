package shop.dear.recommendation.behavior.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.recommendation.behavior.domain.model.RecommendationItem;

import java.util.List;

public interface RecommendationItemJpaRepository
        extends JpaRepository<RecommendationItem, Long> {

    List<RecommendationItem>
    findByCategoryInAndStatus(
            List<ProductCategory> categories,
            ProductStatus status
    );

    List<RecommendationItem>
    findAllByProductIdIn(List<Long> productIds);
}
