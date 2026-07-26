package shop.dear.commerce.order.offer.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.dear.commerce.order.offer.application.OfferService;
import shop.dear.common.auth.AuthUser;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OfferController.class)
class OfferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OfferService offerService;

    @Test
    @DisplayName("오퍼가 존재하면 exists true와 함께 200을 반환한다")
    void returnsExistsTrue() throws Exception {
        // given
        given(offerService.existsActiveOfferByProductId(anyLong())).willReturn(true);

        // when & then
        mockMvc.perform(get("/api/offers/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exists").value(true));
    }

    @Test
    @DisplayName("오퍼가 존재하지 않으면 exists false와 함께 200을 반환한다")
    void returnsExistsFalse() throws Exception {
        // given
        given(offerService.existsActiveOfferByProductId(anyLong())).willReturn(false);

        // when & then
        mockMvc.perform(get("/api/offers/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exists").value(false));
    }

    @Test
    @DisplayName("productId가 0 이하면 400을 반환한다")
    void returnsBadRequest_whenProductIdNotPositive() throws Exception {
        // when & then
        mockMvc.perform(get("/api/offers/0/status"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("오퍼 수락 요청 시 200을 반환한다")
    void returnsOk_whenAcceptOffer() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/offers/1/accept")
                        .header(AuthUser.MEMBER_ID_HEADER, "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("offerId가 0 이하면 400을 반환한다")
    void returnsBadRequest_whenOfferIdNotPositive() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/offers/0/accept")
                        .header(AuthUser.MEMBER_ID_HEADER, "1"))
                .andExpect(status().isBadRequest());
    }
}
