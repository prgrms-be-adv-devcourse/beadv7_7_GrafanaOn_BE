package shop.dear.commerce.financial.payment.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.dear.commerce.financial.payment.application.PaymentService;
import shop.dear.commerce.financial.payment.application.dto.PayOrderCommand;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalPaymentController.class)
class InternalPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void requestPayment_callsPaymentServiceWithPayOrderCommand() throws Exception {
        mockMvc.perform(post("/internal/payments/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 100,
                                  "orderType": "PURCHASE",
                                  "memberId": 1,
                                  "amount": 10000.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("success"));

        verify(paymentService).payOrder(new PayOrderCommand(
                1L,
                100L,
                "PURCHASE",
                new BigDecimal("10000.00")
        ));
    }
}
