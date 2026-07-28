package shop.dear.commerce.financial.payment.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.event.order.OrderType;
import shop.dear.commerce.financial.payment.application.PaymentService;
import shop.dear.commerce.financial.payment.application.dto.PayOrderCommand;
import shop.dear.commerce.financial.payment.application.dto.PaymentInfo;
import shop.dear.commerce.financial.payment.domain.constant.PaymentStatus;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
public class PaymentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void createPayment_returnsPendingPayment() throws Exception {
        // given
        given(paymentService.payOrder(any(PayOrderCommand.class)))
                .willReturn(new PaymentInfo(100L, PaymentStatus.PENDING));

        final PaymentRequestBody request = new PaymentRequestBody(
                10L,
                OrderType.PURCHASE,
                new BigDecimal("10000.00")
        );

        // when & then
        mockMvc.perform(post("/api/payments")
                        .header(AuthUser.MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("success"))
                .andExpect(jsonPath("$.data.paymentId").value(100))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(paymentService).payOrder(
                new PayOrderCommand(
                        1L,
                        10L,
                        OrderType.PURCHASE,
                        new BigDecimal("10000.00")
                )
        );
    }

    @Test
    void getPayment_returnsPaymentStatus() throws Exception {
        // given
        given(paymentService.getPayment(1L, 100L))
                .willReturn(new PaymentInfo(100L, PaymentStatus.PAID));

        // when & then
        mockMvc.perform(get("/api/payments/100")
                        .header(AuthUser.MEMBER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("success"))
                .andExpect(jsonPath("$.data.paymentId").value(100))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        verify(paymentService).getPayment(1L, 100L);
    }

    private record PaymentRequestBody(
            Long orderId,
            OrderType orderType,
            BigDecimal amount
    ) {
    }

    @Test
    void getPayment_withNonPositivePaymentId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/payments/0")
                        .header(AuthUser.MEMBER_ID_HEADER, "1"))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).getPayment(anyLong(), anyLong());
    }

    @Test
    void createPayment_withMissingOrderId_returnsBadRequest() throws Exception {
        final PaymentRequestBody request = new PaymentRequestBody(
                null, OrderType.PURCHASE, new BigDecimal("10000.00")
        );

        mockMvc.perform(post("/api/payments")
                        .header(AuthUser.MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).payOrder(any(PayOrderCommand.class));
    }

    @Test
    void createPayment_withNonPositiveOrderId_returnsBadRequest() throws Exception {
        final PaymentRequestBody request = new PaymentRequestBody(
                0L, OrderType.PURCHASE, new BigDecimal("10000.00")
        );

        mockMvc.perform(post("/api/payments")
                        .header(AuthUser.MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).payOrder(any(PayOrderCommand.class));
    }

    @Test
    void createPayment_withMissingAmount_returnsBadRequest() throws Exception {
        final PaymentRequestBody request = new PaymentRequestBody(
                10L, OrderType.PURCHASE, null
        );

        mockMvc.perform(post("/api/payments")
                        .header(AuthUser.MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).payOrder(any(PayOrderCommand.class));
    }

    @Test
    void createPayment_withNonPositiveAmount_returnsBadRequest() throws Exception {
        final PaymentRequestBody request = new PaymentRequestBody(
                10L, OrderType.PURCHASE, BigDecimal.ZERO
        );

        mockMvc.perform(post("/api/payments")
                        .header(AuthUser.MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).payOrder(any(PayOrderCommand.class));
    }
}
