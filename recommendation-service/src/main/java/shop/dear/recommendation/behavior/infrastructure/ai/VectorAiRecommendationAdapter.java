package shop.dear.recommendation.behavior.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.recommendation.application.RecommendationService;
import shop.dear.recommendation.behavior.application.dto.AiRecommendationResult;
import shop.dear.recommendation.behavior.application.port.AiRecommendationPort;
import shop.dear.recommendation.behavior.domain.model.RecommendationItem;
import shop.dear.recommendation.behavior.domain.repository.RecommendationItemRepository;
import shop.dear.recommendation.domain.model.RecommendationSimilarItem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class VectorAiRecommendationAdapter implements AiRecommendationPort {

    private final RecommendationService recommendationService;
    private final RecommendationItemRepository recommendationItemRepository;

    @Override
    public Optional<AiRecommendationResult> recommendSimilar(
            final Long baseProductId,
            final int limit
    ) {
        try {
            List<RecommendationSimilarItem> similarItems =
                    recommendationService.findSimilarItems(baseProductId, limit);

            if (similarItems.isEmpty()) {
                return Optional.empty();
            }

            List<Long> productIds = similarItems.stream()
                    .map(RecommendationSimilarItem::productId)
                    .toList();

            Map<Long, RecommendationItem> itemsByProductId =
                    recommendationItemRepository.findAllByProductIdIn(productIds).stream()
                            .collect(Collectors.toMap(
                                    RecommendationItem::getProductId,
                                    Function.identity()
                            ));

            List<AiRecommendationResult.AiRecommendationItem> validItems =
                    similarItems.stream()
                            .filter(item -> {
                                RecommendationItem product = itemsByProductId.get(item.productId());
                                return product != null && product.getStatus() == ProductStatus.ON_SALE;
                            })
                            .map(item -> new AiRecommendationResult.AiRecommendationItem(
                                    item.productId(),
                                    1.0 - item.distance()
                            ))
                            .toList();

            if (validItems.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new AiRecommendationResult(validItems));
        } catch (Exception exception) {
            log.warn(
                    "벡터 유사 상품 추천 실패. Behavior 추천으로 fallback. baseProductId={}",
                    baseProductId,
                    exception
            );
            return Optional.empty();
        }
    }
}
