package shop.dear.commerce.financial.wallet.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.commerce.financial.wallet.application.WalletService;
import shop.dear.commerce.financial.wallet.application.dto.HoldCommand;
import shop.dear.commerce.financial.wallet.application.dto.ReleaseCommand;
import shop.dear.common.event.financial.PaymentHoldRequestedEvent;
import shop.dear.common.event.financial.PaymentReleaseRequestedEvent;
import shop.dear.common.response.ApiResponse;

import static shop.dear.common.response.ApiResponse.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/wallets")
public class InternalWalletOperationController {

    private final WalletService walletService;

    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<Void>> hold(
            @RequestBody final PaymentHoldRequestedEvent event
    ) {
        walletService.hold(new HoldCommand(
                event.memberId(),
                event.amount(),
                event.orderId()
        ));

        return ResponseEntity.ok(success());
    }

    @PostMapping("/release")
    public ResponseEntity<ApiResponse<Void>> release(
            @RequestBody final PaymentReleaseRequestedEvent event
    ) {
        walletService.release(new ReleaseCommand(
                event.memberId(),
                event.amount(),
                event.orderId()
        ));

        return ResponseEntity.ok(success());
    }
}
