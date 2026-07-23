package shop.dear.commerce.order.purchase.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.dear.commerce.order.purchase.application.PurchaseService;
import shop.dear.commerce.order.purchase.domain.model.Purchase;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PurchaseController.class)
class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PurchaseService purchaseService;

    @Test
    @DisplayName("즉시구매 생성에 성공하면 201을 반환한다")
    void createPurchaseSuccess() throws Exception {
        final Purchase purchase = Purchase.create(
            1L,
            2L,
            10L,
            new BigDecimal("10000"),
            "서울시 강남구",
            OffsetDateTime.now().plusMinutes(5)
        );
        given(purchaseService.createPurchase(any())).willReturn(purchase);

        mockMvc.perform(post("/api/purchases")
                .header("X-Member-Id", "1")
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
        mockMvc.perform(post("/api/purchases")
                .header("X-Member-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new Request(0L, "서울시 강남구"))))
            .andExpect(status().isBadRequest());

        verify(purchaseService, never()).createPurchase(any());
    }

    @Test
    @DisplayName("배송지가 비어 있으면 400을 반환한다")
    void rejectBlankDelivery() throws Exception {
        mockMvc.perform(post("/api/purchases")
                .header("X-Member-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new Request(10L, " "))))
            .andExpect(status().isBadRequest());

        verify(purchaseService, never()).createPurchase(any());
    }

    @Test
    @DisplayName("회원 식별자가 양수가 아니면 400을 반환한다")
    void rejectInvalidBuyerId() throws Exception {
        mockMvc.perform(post("/api/purchases")
                .header("X-Member-Id", "0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new Request(10L, "서울시 강남구"))))
            .andExpect(status().isBadRequest());

        verify(purchaseService, never()).createPurchase(any());
    }

    private record Request(Long productId, String delivery) {
    }
}
