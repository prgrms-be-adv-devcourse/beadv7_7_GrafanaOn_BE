package shop.dear.commerce.financial.wallet.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.dear.commerce.financial.wallet.application.WalletService;
import shop.dear.commerce.financial.wallet.application.dto.HoldCommand;
import shop.dear.commerce.financial.wallet.application.dto.ReleaseCommand;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalWalletOperationController.class)
class InternalWalletOperationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @Test
    void hold_callsWalletServiceWithHoldCommand() throws Exception {
        mockMvc.perform(post("/internal/wallets/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 200,
                                  "memberId": 2,
                                  "amount": 3000.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("success"));

        verify(walletService).hold(new HoldCommand(
                2L,
                new BigDecimal("3000.00"),
                200L
        ));
    }

    @Test
    void release_callsWalletServiceWithReleaseCommand() throws Exception {
        mockMvc.perform(post("/internal/wallets/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 200,
                                  "memberId": 2,
                                  "amount": 3000.00,
                                  "reason": "OFFER_CANCELLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("success"));

        verify(walletService).release(new ReleaseCommand(
                2L,
                new BigDecimal("3000.00"),
                200L
        ));
    }
}
