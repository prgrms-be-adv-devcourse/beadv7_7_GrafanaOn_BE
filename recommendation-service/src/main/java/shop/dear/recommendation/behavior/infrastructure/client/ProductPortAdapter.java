package shop.dear.recommendation.behavior.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.recommendation.behavior.application.port.ProductPort;
import shop.dear.recommendation.behavior.domain.model.RecommendationItem;
import shop.dear.recommendation.behavior.domain.repository.RecommendationItemRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductPortAdapter implements ProductPort {

    private final RecommendationItemRepository recommendationItemRepository;

    @Override
    public Map<Long, ProductCategory> getProductCategories(
            final List<Long> productIds
    ) {
        return recommendationItemRepository
                .findAllByProductIdIn(productIds)
                .stream()
                .collect(Collectors.toMap(
                        RecommendationItem::getProductId,
                        RecommendationItem::getCategory
                ));
    }
}