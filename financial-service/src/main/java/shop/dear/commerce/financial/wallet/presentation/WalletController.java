package shop.dear.commerce.financial.wallet.presentation;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;
import shop.dear.commerce.financial.wallet.application.WalletService;
import shop.dear.commerce.financial.wallet.application.dto.GetWalletQuery;
import shop.dear.commerce.financial.wallet.application.dto.WalletInfo;
import shop.dear.commerce.financial.wallet.presentation.dto.response.WalletResponse;

import static shop.dear.common.response.ApiResponse.successWithData;

@RequiredArgsConstructor
@RequestMapping("/api/deposits")
@RestController
public class WalletController {
    private final WalletService walletService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(
            @AuthUser final Long memberId
    ) {
        final WalletInfo walletInfo = walletService.getWallet(
                new GetWalletQuery(memberId)
        );

        final WalletResponse walletResponse = WalletResponse.from(walletInfo);

        return ResponseEntity.ok(successWithData(walletResponse));
    }
}
