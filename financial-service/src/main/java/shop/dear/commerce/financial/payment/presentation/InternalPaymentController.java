package shop.dear.commerce.financial.payment.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.commerce.financial.payment.application.PaymentService;
import shop.dear.commerce.financial.payment.application.dto.PayOrderCommand;
import shop.dear.common.event.financial.PaymentRequestedEvent;
import shop.dear.common.response.ApiResponse;

import static shop.dear.common.response.ApiResponse.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/payments")
public class InternalPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<Void>> requestPayment(
            @RequestBody final PaymentRequestedEvent event
    ) {
        paymentService.payOrder(new PayOrderCommand(
                event.memberId(),
                event.orderId(),
                event.orderType(),
                event.amount()
        ));

        return ResponseEntity.ok(success());
    }
}
