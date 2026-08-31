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
import shop.dear.recommendation.behavior.application.port.AiRecommendationPort;
import shop.dear.recommendation.behavior.domain.model.RecommendationItem;
import shop.dear.recommendation.behavior.domain.model.UserInterest;
import shop.dear.recommendation.behavior.application.dto.AiRecommendationResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RecommendationFacadeServiceTest {

    @Mock
    private BasicRecommendationService basicRecommendationService;

    @Mock
    private AiRecommendationPort aiRecommendationPort;

    @InjectMocks
    private RecommendationFacadeService recommendationFacadeService;

    @Test
    @DisplayName("AI 결과가 부족하면 중복 없이 Behavior 추천으로 보충한다")
    void returnsAiRerank_whenAiSucceeds() {
        // given
        UserInterest interest = UserInterest.create(
                1L,
                ProductCategory.SNEAKERS,
                10.0,
                LocalDateTime.now()
        );
        RecommendationItem item1 = RecommendationItem.create(
                100L, ProductCategory.SNEAKERS, ProductStatus.ON_SALE, 10L
        );
        RecommendationItem item2 = RecommendationItem.create(
                200L, ProductCategory.SNEAKERS, ProductStatus.ON_SALE, 10L
        );
        RecommendationContext context = new RecommendationContext(
                List.of(interest),
                List.of(item1, item2)
        );

        given(basicRecommendationService.createContext(1L)).willReturn(context);
        given(basicRecommendationService.recommend(any(RecommendationContext.class), anyString(), eq(2)))
                .willAnswer(invocation -> new BasicRecommendationResponse(
                        invocation.getArgument(1),
                        List.of(
                                new RecommendationItemResponse(100L, 10.0, 1),
                                new RecommendationItemResponse(200L, 5.0, 2)
                        )
                ));
        given(aiRecommendationPort.recommendSimilar(100L, 2))
                .willReturn(Optional.of(new AiRecommendationResult(
                        List.of(
                                new AiRecommendationResult.AiRecommendationItem(200L, 0.9)
                        )
                )));

        // when
        BasicRecommendationResponse response = recommendationFacadeService.recommend(1L, 2);

        // then
        assertThat(response.recommendationId()).isNotNull();
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).productId()).isEqualTo(200L);
        assertThat(response.items().get(0).rank()).isEqualTo(1);
        assertThat(response.items().get(1).productId()).isEqualTo(100L);
        assertThat(response.items().get(1).rank()).isEqualTo(2);
    }

    @Test
    @DisplayName("AI rerank 실패 시 behavior 결과를 동일 recommendationId로 반환한다")
    void fallsBackToBehavior_whenAiFails() {
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
        RecommendationContext context = new RecommendationContext(
                List.of(interest),
                List.of(item)
        );

        given(basicRecommendationService.createContext(1L)).willReturn(context);
        given(basicRecommendationService.recommend(any(RecommendationContext.class), anyString(), eq(10)))
                .willAnswer(invocation -> new BasicRecommendationResponse(
                        invocation.getArgument(1),
                        List.of(new RecommendationItemResponse(100L, 10.0, 1))
                ));
        given(aiRecommendationPort.recommendSimilar(100L, 10))
                .willReturn(Optional.empty());

        // when
        BasicRecommendationResponse response = recommendationFacadeService.recommend(1L, 10);

        // then
        assertThat(response.recommendationId()).isNotNull();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("context가 비어있으면 빈 추천을 반환한다")
    void returnsEmpty_whenContextIsEmpty() {
        // given
        RecommendationContext context = new RecommendationContext(List.of(), List.of());
        given(basicRecommendationService.createContext(1L)).willReturn(context);

        // when
        BasicRecommendationResponse response = recommendationFacadeService.recommend(1L, 10);

        // then
        assertThat(response).isEqualTo(BasicRecommendationResponse.empty());
    }
}
