package shop.dear.recommendation.behavior.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.recommendation.behavior.application.dto.BasicRecommendationResponse;
import shop.dear.recommendation.behavior.application.dto.RecommendationContext;
import shop.dear.recommendation.behavior.application.dto.RecommendationItemResponse;
import shop.dear.recommendation.behavior.domain.model.RecommendationItem;
import shop.dear.recommendation.behavior.domain.model.UserInterest;
import shop.dear.recommendation.behavior.domain.repository.RecommendationItemRepository;
import shop.dear.recommendation.behavior.domain.repository.UserInterestRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BasicRecommendationServiceTest {

    @Mock
    private UserInterestRepository userInterestRepository;

    @Mock
    private RecommendationItemRepository recommendationItemRepository;

    @InjectMocks
    private BasicRecommendationService basicRecommendationService;

    @Test
    @DisplayName("관심 카테고리가 없으면 빈 추천을 반환한다")
    void returnsEmpty_whenNoInterests() {
        given(userInterestRepository.findByMemberIdOrderByScoreDesc(1L))
                .willReturn(List.of());

        RecommendationContext context = basicRecommendationService.createContext(1L);
        BasicRecommendationResponse response = basicRecommendationService.recommend(context, "test", 10);

        assertThat(response.items()).isEmpty();
    }

    @Test
    @DisplayName("상위 관심 카테고리의 ON_SALE 상품을 점수 순으로 랭킹한다")
    void ranksCandidatesByCategoryScoreAndPopularity() {
        // given
        UserInterest interest = UserInterest.create(
                1L,
                ProductCategory.SNEAKERS,
                10.0,
                LocalDateTime.now()
        );

        RecommendationItem item1 = RecommendationItem.create(
                100L, ProductCategory.SNEAKERS, ProductStatus.ON_SALE, 1000L
        );
        RecommendationItem item2 = RecommendationItem.create(
                200L, ProductCategory.SNEAKERS, ProductStatus.ON_SALE, 10L
        );

        given(userInterestRepository.findByMemberIdOrderByScoreDesc(1L))
                .willReturn(List.of(interest));
        given(recommendationItemRepository.findCandidates(List.of(ProductCategory.SNEAKERS)))
                .willReturn(List.of(item1, item2));

        // when
        RecommendationContext context = basicRecommendationService.createContext(1L);
        BasicRecommendationResponse response = basicRecommendationService.recommend(context, "test", 10);

        // then
        List<RecommendationItemResponse> items = response.items();
        assertThat(items).hasSize(2);
        assertThat(items.get(0).productId()).isEqualTo(100L);
        assertThat(items.get(1).productId()).isEqualTo(200L);
        assertThat(items.get(0).rank()).isEqualTo(1);
    }

    @Test
    @DisplayName("외부에서 전달한 recommendationId를 그대로 응답에 담는다")
    void preservesGivenRecommendationId() {
        // given
        UserInterest interest = UserInterest.create(
                1L,
                ProductCategory.SNEAKERS,
                10.0,
                LocalDateTime.now()
        );
        RecommendationItem item = RecommendationItem.create(
                100L, ProductCategory.SNEAKERS, ProductStatus.ON_SALE, 10L
        );

        given(userInterestRepository.findByMemberIdOrderByScoreDesc(1L))
                .willReturn(List.of(interest));
        given(recommendationItemRepository.findCandidates(List.of(ProductCategory.SNEAKERS)))
                .willReturn(List.of(item));

        RecommendationContext context = basicRecommendationService.createContext(1L);
        BasicRecommendationResponse response = basicRecommendationService.recommend(context, "test", 10);

        // then
        assertThat(response.recommendationId()).isEqualTo("test");
        assertThat(response.items()).hasSize(1);
    }
}