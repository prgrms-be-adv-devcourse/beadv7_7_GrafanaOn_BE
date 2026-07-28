package shop.dear.commerce.order.offer.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.dear.commerce.order.offer.application.OfferService;
import shop.dear.commerce.order.offer.domain.model.Offer;
import shop.dear.commerce.order.offer.presentation.dto.CreateOfferRequest;
import shop.dear.commerce.order.offer.presentation.dto.CreateOfferSnapshotRequest;
import shop.dear.commerce.order.offersnapshot.domain.model.OfferSnapshot;
import shop.dear.common.auth.AuthUser;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OfferController.class)
class OfferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OfferService offerService;

    @Test
    @DisplayName("오퍼 스냅샷 생성 요청 시 201을 반환한다")
    void returnsCreated_whenCreateOfferSnapshot() throws Exception {
        // given
        final CreateOfferSnapshotRequest request = new CreateOfferSnapshotRequest(1L);
        given(offerService.createOfferSnapshot(any()))
                .willReturn(OfferSnapshot.create(1L, 1L, 1L, "MODEL-001", BigDecimal.valueOf(10000)));

        // when & then
        mockMvc.perform(post("/api/offers/snapshot")
                        .header(AuthUser.MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("오퍼 스냅샷 생성 시 productId가 없으면 400을 반환한다")
    void returnsBadRequest_whenCreateOfferSnapshotWithoutProductId() throws Exception {
        // given
        final String request = objectMapper.writeValueAsString(new java.util.HashMap<>());

        // when & then
        mockMvc.perform(post("/api/offers/snapshot")
                        .header(AuthUser.MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("오퍼 생성 요청 시 201을 반환한다")
    void returnsCreated_whenCreateOffer() throws Exception {
        // given
        final CreateOfferRequest request = new CreateOfferRequest(1L, "title", "story", "delivery");
        final Offer offer = Offer.create(
                2L, 3L, 1L, 10L, new BigDecimal("10000"), "title", "story", "delivery"
        );
        given(offerService.createOffer(any()))
                .willReturn(offer);

        // when & then
        mockMvc.perform(post("/api/offers")
                        .header(AuthUser.MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("오퍼 생성 시 snapshotId가 없으면 400을 반환한다")
    void returnsBadRequest_whenCreateOfferWithoutSnapshotId() throws Exception {
        // given
        final String request = "{\"buyerId\": 2, \"title\": \"title\", \"story\": \"story\", \"delivery\": \"delivery\"}";

        // when & then
        mockMvc.perform(post("/api/offers")
                        .header(AuthUser.MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
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
