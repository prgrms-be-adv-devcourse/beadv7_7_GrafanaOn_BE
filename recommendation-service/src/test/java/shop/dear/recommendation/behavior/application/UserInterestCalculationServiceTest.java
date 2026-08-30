package shop.dear.recommendation.behavior.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.recommendation.behavior.application.port.ProductPort;
import shop.dear.recommendation.behavior.domain.constant.BehaviorType;
import shop.dear.recommendation.behavior.domain.model.RecommendationBehaviorEvent;
import shop.dear.recommendation.behavior.domain.model.UserInterest;
import shop.dear.recommendation.behavior.domain.policy.BehaviorWeightPolicy;
import shop.dear.recommendation.behavior.domain.repository.BehaviorEventRepository;
import shop.dear.recommendation.behavior.domain.repository.UserInterestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserInterestCalculationServiceTest {

    @Mock
    private BehaviorEventRepository behaviorEventRepository;

    @Mock
    private UserInterestRepository userInterestRepository;

    @Mock
    private ProductPort productPort;

    @Mock
    private BehaviorWeightPolicy behaviorWeightPolicy;

    @InjectMocks
    private UserInterestCalculationService userInterestCalculationService;

    @Test
    @DisplayName("관심 이벤트가 없으면 저장하지 않는다")
    void doesNothing_whenNoInterestEvents() {
        given(behaviorEventRepository.findByMemberIdAndOccurredAtAfter(1L, LocalDateTime.MIN))
                .willReturn(List.of());

        userInterestCalculationService.calculateUserInterests(1L, LocalDateTime.MIN);

        verify(userInterestRepository, never()).save(any());
    }

    @Test
    @DisplayName("카테고리별 가중치 합산 점수를 저장한다")
    void calculatesAndSavesCategoryScores() {
        // given
        LocalDateTime since = LocalDateTime.of(2026, 8, 1, 0, 0);
        RecommendationBehaviorEvent event = RecommendationBehaviorEvent.create(
                "evt-1", null, 1L, 100L, BehaviorType.VIEW, since.plusDays(1)
        );

        given(behaviorEventRepository.findByMemberIdAndOccurredAtAfter(1L, since))
                .willReturn(List.of(event));
        given(behaviorWeightPolicy.contributesToInterest(BehaviorType.VIEW))
                .willReturn(true);
        given(behaviorWeightPolicy.getWeight(BehaviorType.VIEW))
                .willReturn(1.0);
        given(productPort.getProductCategories(List.of(100L)))
                .willReturn(Map.of(100L, ProductCategory.SNEAKERS));

        // when
        userInterestCalculationService.calculateUserInterests(1L, since);

        // then
        ArgumentCaptor<UserInterest> captor = ArgumentCaptor.forClass(UserInterest.class);
        verify(userInterestRepository).save(captor.capture());
        UserInterest saved = captor.getValue();
        assertThat(saved.getMemberId()).isEqualTo(1L);
        assertThat(saved.getCategory()).isEqualTo(ProductCategory.SNEAKERS);
        assertThat(saved.getScore()).isEqualTo(1.0);
    }
}