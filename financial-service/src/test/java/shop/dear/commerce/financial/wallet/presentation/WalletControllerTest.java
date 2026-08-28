package shop.dear.commerce.financial.wallet.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.dear.common.auth.AuthUser;
import shop.dear.commerce.financial.wallet.application.WalletService;
import shop.dear.commerce.financial.wallet.application.dto.GetWalletQuery;
import shop.dear.commerce.financial.wallet.application.dto.WalletInfo;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
public class WalletControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @Test
    @DisplayName("인증된 회원이 예치금을 조회하면 잔액 정보를 반환한다.")
    void getMyWallet_whenAuthenticated_returnsWalletBalance() throws Exception {
        when(walletService.getWallet(new GetWalletQuery(1L)))
                .thenReturn(new WalletInfo(
                        1L,
                        BigDecimal.valueOf(10_000),
                        BigDecimal.valueOf(6_000)
                ));

        mockMvc.perform(get("/api/deposits/me")
                        .header(AuthUser.MEMBER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableBalance").value(10_000))
                .andExpect(jsonPath("$.data.heldBalance").value(6_000));
    }
}
