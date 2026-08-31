package shop.dear.recommendation.behavior.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import shop.dear.recommendation.behavior.application.dto.AiRecommendationResult;
import shop.dear.recommendation.behavior.application.dto.BasicRecommendationResponse;
import shop.dear.recommendation.behavior.application.dto.RecommendationContext;
import shop.dear.recommendation.behavior.application.dto.RecommendationItemResponse;
import shop.dear.recommendation.behavior.application.port.AiRecommendationPort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationFacadeService {

    private static final int MAX_RECOMMENDATION_LIMIT = 50;

    private final BasicRecommendationService basicRecommendationService;
    private final AiRecommendationPort aiRecommendationPort;

    public BasicRecommendationResponse recommend(
            final Long memberId,
            final int limit
    ) {
        validateLimit(limit);
        RecommendationContext context =
                basicRecommendationService.createContext(memberId);

        if (context.isEmpty()) {
            return BasicRecommendationResponse.empty();
        }

        String recommendationId = UUID.randomUUID().toString();

        BasicRecommendationResponse behaviorResult =
                basicRecommendationService.recommend(
                        context,
                        recommendationId,
                        limit
                );

        if (behaviorResult.items().isEmpty()) {
            return behaviorResult;
        }

        Long baseProductId = behaviorResult.items().getFirst().productId();
        Optional<AiRecommendationResult> aiResult =
                aiRecommendationPort.recommendSimilar(
                        baseProductId,
                        limit
                );

        if (aiResult.isEmpty() || aiResult.get().isEmpty()) {

            log.info(
                    "AI 추천 결과가 없어 Behavior 추천으로 fallback. memberId={}",
                    memberId
            );

            return behaviorResult;
        }

        return mergeResults(
                recommendationId,
                aiResult.get(),
                behaviorResult,
                limit
        );
    }

    private BasicRecommendationResponse mergeResults(
            final String recommendationId,
            final AiRecommendationResult aiResult,
            final BasicRecommendationResponse behaviorResult,
            final int limit
    ) {
        Map<Long, Double> scoresByProductId = new LinkedHashMap<>();

        aiResult.items().forEach(item ->
                scoresByProductId.putIfAbsent(item.productId(), item.score())
        );
        behaviorResult.items().forEach(item ->
                scoresByProductId.putIfAbsent(item.productId(), item.score())
        );

        List<Map.Entry<Long, Double>> entries =
                new ArrayList<>(scoresByProductId.entrySet());
        List<RecommendationItemResponse> items =
                java.util.stream.IntStream.range(
                                0,
                                Math.min(limit, entries.size())
                        )
                        .mapToObj(index -> new RecommendationItemResponse(
                                entries.get(index).getKey(),
                                entries.get(index).getValue(),
                                index + 1
                        ))
                        .toList();

        return new BasicRecommendationResponse(recommendationId, items);
    }

    private void validateLimit(
            final int limit
    ) {
        if (limit <= 0 || limit > MAX_RECOMMENDATION_LIMIT) {
            throw new IllegalArgumentException(
                    "추천 개수는 1 이상 "
                            + MAX_RECOMMENDATION_LIMIT
                            + " 이하여야 합니다."
            );
        }
    }
}