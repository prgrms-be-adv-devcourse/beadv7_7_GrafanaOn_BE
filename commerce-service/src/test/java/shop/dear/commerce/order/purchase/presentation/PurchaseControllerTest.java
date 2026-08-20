package shop.dear.commerce.order.purchase.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.dear.commerce.order.purchase.application.PurchaseService;
import shop.dear.commerce.order.purchase.domain.model.Purchase;
import shop.dear.common.auth.AuthUser;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PurchaseController.class)
class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PurchaseService purchaseService;

    @Test
    @DisplayName("즉시구매 생성에 성공하면 201을 반환한다")
    void createPurchaseSuccess() throws Exception {
        // given
        final Purchase purchase = Purchase.create(
                1L,
                2L,
                10L,
                new BigDecimal("10000"),
                "서울시 강남구",
                LocalDateTime.now()
        );
        given(purchaseService.createPurchase(any())).willReturn(purchase);

        // when & then
        mockMvc.perform(post("/api/purchases")
                        .header(AuthUser.MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Request(10L, "서울시 강남구"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("success"))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.amount").value(10000));
    }

    @Test
    @DisplayName("상품 식별자가 양수가 아니면 400을 반환한다")
    void rejectInvalidProductId() throws Exception {
        // when & then
        mockMvc.perform(post("/api/purchases")
                        .header(AuthUser.MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Request(0L, "서울시 강남구"))))
                .andExpect(status().isBadRequest());

        verify(purchaseService, never()).createPurchase(any());
    }

    @Test
    @DisplayName("배송지가 비어 있으면 400을 반환한다")
    void rejectBlankDelivery() throws Exception {
        // when & then
        mockMvc.perform(post("/api/purchases")
                        .header(AuthUser.MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Request(10L, " "))))
                .andExpect(status().isBadRequest());

        verify(purchaseService, never()).createPurchase(any());
    }

    @Test
    @DisplayName("즉시구매 취소에 성공하면 200을 반환한다")
    void cancelPurchaseSuccess() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/purchases/1/cancel")
                        .header(AuthUser.MEMBER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("success"));

        verify(purchaseService).cancelPurchase(1L, 1L);
    }

    @Test
    @DisplayName("구매 식별자가 양수가 아니면 400을 반환한다")
    void rejectInvalidPurchaseId() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/purchases/0/cancel")
                        .header(AuthUser.MEMBER_ID_HEADER, "1"))
                .andExpect(status().isBadRequest());

        verify(purchaseService, never()).cancelPurchase(anyLong(), anyLong());
    }

    @Test
    @DisplayName("내 즉시구매 목록 조회에 성공하면 200을 반환한다")
    void getPurchasesByMeSuccess() throws Exception {
        // given
        final Purchase purchase1 = Purchase.create(
                1L,
                2L,
                10L,
                new BigDecimal("10000"),
                "서울시 강남구",
                LocalDateTime.now().plusMinutes(5)
        );
        final Purchase purchase2 = Purchase.create(
                1L,
                3L,
                11L,
                new BigDecimal("20000"),
                "서울시 서초구",
                LocalDateTime.now().plusMinutes(5)
        );
        given(purchaseService.getPurchasesByBuyerId(any(), any()))
                .willReturn(new PageImpl<>(List.of(purchase1, purchase2), PageRequest.of(0, 10), 2));

        // when & then
        mockMvc.perform(get("/api/purchases/me")
                        .header(AuthUser.MEMBER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("success"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].productId").value(10))
                .andExpect(jsonPath("$.data.content[0].amount").value(10000))
                .andExpect(jsonPath("$.data.content[1].productId").value(11))
                .andExpect(jsonPath("$.data.content[1].amount").value(20000));
    }

    @Test
    @DisplayName("즉시구매 상세 조회에 성공하면 200을 반환한다")
    void getPurchaseSuccess() throws Exception {
        // given
        final Purchase purchase = Purchase.create(
                1L,
                2L,
                10L,
                new BigDecimal("10000"),
                "서울시 강남구",
                LocalDateTime.now().plusMinutes(5)
        );
        given(purchaseService.getPurchase(1L, 1L)).willReturn(purchase);

        // when & then
        mockMvc.perform(get("/api/purchases/1")
                        .header(AuthUser.MEMBER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("success"))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.productId").value(10))
                .andExpect(jsonPath("$.data.amount").value(10000));
    }

    @Test
    @DisplayName("구매 식별자가 양수가 아니면 400을 반환한다")
    void rejectInvalidPurchaseIdForDetail() throws Exception {
        // when & then
        mockMvc.perform(get("/api/purchases/0")
                        .header(AuthUser.MEMBER_ID_HEADER, "1"))
                .andExpect(status().isBadRequest());

        verify(purchaseService, never()).getPurchase(anyLong(), anyLong());
    }

    private record Request(Long productId, String delivery) {
    }
}
