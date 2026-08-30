package shop.dear.recommendation.behavior.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.recommendation.behavior.application.port.ProductPort;
import shop.dear.recommendation.behavior.domain.model.RecommendationBehaviorEvent;
import shop.dear.recommendation.behavior.domain.model.UserInterest;
import shop.dear.recommendation.behavior.domain.policy.BehaviorWeightPolicy;
import shop.dear.recommendation.behavior.domain.repository.BehaviorEventRepository;
import shop.dear.recommendation.behavior.domain.repository.UserInterestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * 관심도 계산
 */
@Service
@RequiredArgsConstructor
public class UserInterestCalculationService {

    private final BehaviorEventRepository behaviorEventRepository;
    private final UserInterestRepository userInterestRepository;
    private final ProductPort productPort;
    private final BehaviorWeightPolicy behaviorWeightPolicy;

    @Transactional
    public void calculateUserInterests(final Long memberId, final LocalDateTime since) {
        //해당 기간 내 행동 이벤트 조회
        List<RecommendationBehaviorEvent> events =
                behaviorEventRepository.
                        findByMemberIdAndOccurredAtAfter(
                                memberId,
                                since
                        );
        List<RecommendationBehaviorEvent> interestEvents = events.stream()
                .filter(event ->
                        behaviorWeightPolicy
                                .contributesToInterest(
                                        event.getEventType()
                                )
                ).toList();
        if(interestEvents.isEmpty()) {
            return;
        }
        Map<Long, ProductCategory> productCategories = fetchProductCategories(events);
        Map<ProductCategory, Double> categoryScores = calculateCategoryScores(interestEvents, productCategories);

        saveUserInterests(memberId, categoryScores);
    }

    private Map<Long, ProductCategory> fetchProductCategories(
            final List<RecommendationBehaviorEvent> events
    ) {
        List<Long> productIds = events.stream()
                .map(RecommendationBehaviorEvent::getProductId)
                .distinct()
                .toList();

        return productPort.getProductCategories(productIds);
    }

    private Map<ProductCategory, Double> calculateCategoryScores(
            final List<RecommendationBehaviorEvent> events,
            final Map<Long, ProductCategory> productCategories
    ) {
        return events.stream()
                .filter(event ->
                        productCategories.containsKey(
                                event.getProductId()
                        )
                )
                .collect(Collectors.groupingBy(
                        event -> productCategories.get(event.getProductId()),
                        Collectors.summingDouble(event ->
                                behaviorWeightPolicy.getWeight(
                                        event.getEventType()
                                )
                        )
                ));
    }

    private void saveUserInterests(
            final Long memberId,
            final Map<ProductCategory, Double> categoryScores
    ) {
        LocalDateTime calculatedAt = LocalDateTime.now();

        categoryScores.forEach((category, score) -> {
            UserInterest userInterest =
                    userInterestRepository.findByMemberIdAndCategory(
                            memberId,
                            category
                    ).orElseGet(() ->
                            UserInterest.create(
                                    memberId,
                                    category,
                                    score,
                                    calculatedAt
                            )
                    );
            if(userInterest.getId() != null) {
                userInterest.update(
                        score,
                        calculatedAt
                );
            }
            userInterestRepository.save(userInterest);
        });
    }
}
