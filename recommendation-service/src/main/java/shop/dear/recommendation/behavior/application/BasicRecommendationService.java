package shop.dear.recommendation.behavior.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.recommendation.behavior.application.dto.BasicRecommendationResponse;
import shop.dear.recommendation.behavior.application.dto.RecommendationContext;
import shop.dear.recommendation.behavior.application.dto.RecommendationItemResponse;
import shop.dear.recommendation.behavior.domain.model.RecommendationItem;
import shop.dear.recommendation.behavior.domain.model.UserInterest;
import shop.dear.recommendation.behavior.domain.repository.RecommendationItemRepository;
import shop.dear.recommendation.behavior.domain.repository.UserInterestRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BasicRecommendationService {

    private static final int TOP_CATEGORY_LIMIT = 3;

    private final UserInterestRepository userInterestRepository;
    private final RecommendationItemRepository recommendationItemRepository;

    public RecommendationContext createContext(
            final Long memberId
    ) {
        List<UserInterest> interests = userInterestRepository.findByMemberIdOrderByScoreDesc(memberId);

        if (interests.isEmpty()) {
            return RecommendationContext.empty();
        }

        List<ProductCategory> topCategories = extractTopCategories(interests);

        List<RecommendationItem> candidates = recommendationItemRepository.findCandidates(topCategories);

        if (candidates.isEmpty()) {
            return RecommendationContext.empty();
        }

        return new RecommendationContext(
                interests,
                candidates
        );
    }

    public BasicRecommendationResponse recommend(
            final RecommendationContext context,
            final String recommendationId,
            final int limit
    ) {
        if (context.isEmpty()) {
            return BasicRecommendationResponse.empty();
        }

        Map<ProductCategory, Double> interestScoreMap =
                createInterestScoreMap(
                        context.interests()
                );

        List<RecommendationItemResponse> items =
                rankCandidates(
                        context.candidates(),
                        interestScoreMap,
                        limit
                );

        return new BasicRecommendationResponse(
                recommendationId,
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
                .collect(
                        Collectors.toMap(
                                UserInterest::getCategory,
                                UserInterest::getScore
                        )
                );
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

        List<RecommendationItemResponse> result = new ArrayList<>();

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

        double popularityScore = Math.log1p(
                        item.getViewCount()
                );

        return categoryScore + popularityScore;
    }

    private record ScoredItem(
            Long productId,
            double score
    ) {
    }
}