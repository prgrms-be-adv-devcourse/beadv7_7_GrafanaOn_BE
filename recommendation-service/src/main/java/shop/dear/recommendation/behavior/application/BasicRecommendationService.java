package shop.dear.recommendation.behavior.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.recommendation.behavior.application.dto.BasicRecommendationResponse;
import shop.dear.recommendation.behavior.application.dto.RecommendationItemResponse;
import shop.dear.recommendation.behavior.domain.model.RecommendationItem;
import shop.dear.recommendation.behavior.domain.model.UserInterest;
import shop.dear.recommendation.behavior.domain.repository.RecommendationItemRepository;
import shop.dear.recommendation.behavior.domain.repository.UserInterestRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/* V1 (초기 추천 알고리즘)
 * UserInterest 상위 카테고리 조회
   -> 카테고리 ON_SALE 상품 후보 조회
   -> 사용자 관심도 + 상품 viewCount 기반 점수 계산
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BasicRecommendationService {

    private static final int TOP_CATEGORY_LIMIT = 3;

    private final UserInterestRepository userInterestRepository;
    private final RecommendationItemRepository recommendationItemRepository;

    public BasicRecommendationResponse recommend(
            final Long memberId,
            final int limit
    ) {
        List<UserInterest> interests =
                userInterestRepository.findByMemberIdOrderByScoreDesc(memberId);

        if (interests.isEmpty()) {
            return BasicRecommendationResponse.empty();
        }

        List<ProductCategory> topCategories =
                extractTopCategories(interests);

        Map<ProductCategory, Double> interestScoreMap =
                createInterestScoreMap(interests);

        List<RecommendationItem> candidates =
                recommendationItemRepository.findCandidates(topCategories);

        if (candidates.isEmpty()) {
            return BasicRecommendationResponse.empty();
        }

        List<RecommendationItemResponse> items =
                rankCandidates(
                        candidates,
                        interestScoreMap,
                        limit
                );

        return new BasicRecommendationResponse(
                UUID.randomUUID().toString(),
                items
        );
    }

    private List<ProductCategory> extractTopCategories(
            final List<UserInterest> interests
    ) {
        return interests.stream()
                .limit(TOP_CATEGORY_LIMIT)
                .map(UserInterest::getCategory)
                .toList();
    }

    private Map<ProductCategory, Double> createInterestScoreMap(
            final List<UserInterest> interests
    ) {
        return interests.stream()
                .collect(Collectors.toMap(
                        UserInterest::getCategory,
                        UserInterest::getScore
                ));
    }

    private List<RecommendationItemResponse> rankCandidates(
            final List<RecommendationItem> candidates,
            final Map<ProductCategory, Double> interestScoreMap,
            final int limit
    ) {
        List<ScoredItem> scoredItems =
                candidates.stream()
                        .map(item ->
                                new ScoredItem(
                                        item.getProductId(),
                                        calculateScore(
                                                item,
                                                interestScoreMap
                                        )
                                )
                        )
                        .sorted(
                                Comparator.comparingDouble(
                                        ScoredItem::score
                                ).reversed()
                        )
                        .limit(limit)
                        .toList();

        List<RecommendationItemResponse> result =
                new ArrayList<>();

        for (int i = 0; i < scoredItems.size(); i++) {
            ScoredItem item = scoredItems.get(i);

            result.add(
                    new RecommendationItemResponse(
                            item.productId(),
                            item.score(),
                            i + 1
                    )
            );
        }

        return result;
    }

    private double calculateScore(
            final RecommendationItem item,
            final Map<ProductCategory, Double> interestScoreMap
    ) {
        double categoryScore =
                interestScoreMap.getOrDefault(
                        item.getCategory(),
                        0.0
                );

        double popularityScore =
                Math.log1p(item.getViewCount());

        return categoryScore + popularityScore;
    }

    private record ScoredItem(
            Long productId,
            double score
    ) {
    }
}