package shop.dear.commerce.financial.wallet.presentation;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.commerce.financial.wallet.application.WalletService;
import shop.dear.commerce.financial.wallet.application.dto.GetWalletInfo;
import shop.dear.commerce.financial.wallet.application.dto.GetWalletQuery;
import shop.dear.commerce.financial.wallet.application.dto.WalletInfo;
import shop.dear.commerce.financial.wallet.presentation.dto.request.WalletRequest;
import shop.dear.commerce.financial.wallet.presentation.dto.response.GetWalletResponse;
import shop.dear.commerce.financial.wallet.presentation.dto.response.WalletResponse;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;

import static shop.dear.common.response.ApiResponse.success;
import static shop.dear.common.response.ApiResponse.successWithData;

@RequiredArgsConstructor
@RequestMapping("/internal/deposits")
@RestController
public class InternalWalletController {
    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<ApiResponse<GetWalletResponse>> getMyWallet(@AuthUser final Long memberId) {

        final GetWalletInfo getWalletInfo = walletService.getWalletId(memberId);

        final GetWalletResponse getWalletResponse = GetWalletResponse.from(getWalletInfo);

        return ResponseEntity.ok(successWithData(getWalletResponse));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createWallet(@Valid @RequestBody final WalletRequest request) {

        walletService.saveWallet(request.memberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(success());
    }
}
