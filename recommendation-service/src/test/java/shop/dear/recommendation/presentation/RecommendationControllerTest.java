package shop.dear.recommendation.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.dear.common.exception.BusinessException;
import shop.dear.recommendation.application.RecommendationService;
import shop.dear.recommendation.domain.exception.RecommendationErrorCode;
import shop.dear.recommendation.domain.model.RecommendationSimilarItem;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest {

	private static final String URI = "/api/recommendations/similar";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RecommendationService recommendationService;

	@Test
	@DisplayName("가까운 순으로 rank 를 1부터 매겨 응답한다")
	void returnsRankedItems() throws Exception {
		given(recommendationService.findSimilarItems(101L, 2)).willReturn(List.of(
			new RecommendationSimilarItem(104L, 0.11d),
			new RecommendationSimilarItem(109L, 0.27d)
		));

		mockMvc.perform(get(URI).param("productId", "101").param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].productId").value(104))
			.andExpect(jsonPath("$.data[0].rank").value(1))
			.andExpect(jsonPath("$.data[1].productId").value(109))
			.andExpect(jsonPath("$.data[1].rank").value(2));
	}

	@Test
	@DisplayName("size 를 생략하면 null 로 넘겨 서비스의 기본값을 쓰게 한다")
	void sizeIsOptional() throws Exception {
		given(recommendationService.findSimilarItems(101L, null)).willReturn(List.of());

		mockMvc.perform(get(URI).param("productId", "101"))
			.andExpect(status().isOk());

		then(recommendationService).should().findSimilarItems(101L, null);
	}

	// 기준 상품에 아직 벡터가 없는 경우다. 상품이 없다는 뜻이 아니므로 404 가 아니다.
	@Test
	@DisplayName("유사 상품이 없으면 200 과 빈 배열을 반환한다")
	void returnsEmptyArrayWhenNoVector() throws Exception {
		given(recommendationService.findSimilarItems(999L, null)).willReturn(List.of());

		mockMvc.perform(get(URI).param("productId", "999"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(0));
	}

	/*
	 * productId 는 @RequestParam(required = false) 로 받고 서비스가 거절한다.
	 * required = true 로 두면 MissingServletRequestParameterException 이
	 * CommonExceptionHandler 의 catch-all 로 떨어져 500 이 나가기 때문이다.
	 */
	@Test
	@DisplayName("productId 가 없으면 400 과 에러 코드를 반환한다")
	void rejectsMissingProductId() throws Exception {
		given(recommendationService.findSimilarItems(null, null))
			.willThrow(new BusinessException(RecommendationErrorCode.PRODUCT_ID_REQUIRED));

		mockMvc.perform(get(URI))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("RC-002"));
	}
}
