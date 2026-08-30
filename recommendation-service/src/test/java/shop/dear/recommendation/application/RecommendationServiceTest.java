package shop.dear.recommendation.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.recommendation.domain.model.RecommendationSimilarItem;
import shop.dear.recommendation.domain.repository.ProductVectorRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

	private static final double MAX_DISTANCE = 0.45d;
	private static final int DEFAULT_SIZE = 10;
	private static final int MAX_SIZE = 50;

	@Mock
	private ProductVectorRepository productVectorRepository;

	private RecommendationService recommendationService;

	@BeforeEach
	void setUp() {
		this.recommendationService = new RecommendationService(
			this.productVectorRepository, MAX_DISTANCE, DEFAULT_SIZE, MAX_SIZE
		);
	}

	@Test
	@DisplayName("Threshold 와 limit 을 그대로 조회 조건으로 넘긴다")
	void passesThresholdAndLimit() {
		this.recommendationService.findSimilarItems(101L, 5);

		then(this.productVectorRepository).should().findSimilar(101L, MAX_DISTANCE, 5);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(ints = {0, -1})
	@DisplayName("size 가 없거나 1 미만이면 기본값을 쓴다")
	void fallsBackToDefaultSize(final Integer size) {
		this.recommendationService.findSimilarItems(101L, size);

		then(this.productVectorRepository).should()
			.findSimilar(101L, MAX_DISTANCE, DEFAULT_SIZE);
	}

	// Index 가 없어 limit 이 곧 조회 비용이다. 요청이 큰 값을 보내도 상한에서 자른다.
	@Test
	@DisplayName("size 가 상한을 넘으면 상한으로 자른다")
	void clampsSizeToMax() {
		this.recommendationService.findSimilarItems(101L, 1_000);

		then(this.productVectorRepository).should()
			.findSimilar(101L, MAX_DISTANCE, MAX_SIZE);
	}

	// 기준 상품에 벡터가 없는 경우다. 예외가 아니라 "추천 없음" 이다.
	@Test
	@DisplayName("조회 결과가 없으면 빈 리스트를 반환한다")
	void returnsEmptyWhenNoMatch() {
		given(this.productVectorRepository.findSimilar(anyLong(), anyDouble(), anyInt()))
			.willReturn(List.of());

		assertThat(this.recommendationService.findSimilarItems(999L, null)).isEmpty();
	}

	@Test
	@DisplayName("저장소가 돌려준 가까운 순서를 그대로 유지한다")
	void keepsRepositoryOrder() {
		given(this.productVectorRepository.findSimilar(anyLong(), anyDouble(), anyInt()))
			.willReturn(List.of(new RecommendationSimilarItem(104L, 0.11d), new RecommendationSimilarItem(109L, 0.27d)));

		final List<RecommendationSimilarItem> items = this.recommendationService.findSimilarItems(101L, null);

		assertThat(items).extracting(RecommendationSimilarItem::productId).containsExactly(104L, 109L);
	}
}
