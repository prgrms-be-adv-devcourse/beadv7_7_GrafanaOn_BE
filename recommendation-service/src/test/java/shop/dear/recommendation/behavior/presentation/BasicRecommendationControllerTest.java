package shop.dear.recommendation.behavior.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.dear.recommendation.behavior.application.BasicRecommendationService;
import shop.dear.recommendation.behavior.application.dto.BasicRecommendationResponse;
import shop.dear.recommendation.behavior.application.dto.RecommendationItemResponse;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BasicRecommendationController.class)
class BasicRecommendationControllerTest {

    private static final String URI = "/api/recommendations";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BasicRecommendationService basicRecommendationService;

    @Test
    @DisplayName("인증 헤더가 있으면 추천 결과를 반환한다")
    void returnsRecommendations_whenAuthenticated() throws Exception {
        BasicRecommendationResponse response = new BasicRecommendationResponse(
                "rec-1",
                List.of(new RecommendationItemResponse(100L, 10.5, 1))
        );
        given(basicRecommendationService.recommend(1L, 10)).willReturn(response);

        mockMvc.perform(get(URI)
                        .header("X-Authenticated-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].productId").value(100));
    }

    @Test
    @DisplayName("인증 헤더가 없으면 401을 반환한다")
    void returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get(URI).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}