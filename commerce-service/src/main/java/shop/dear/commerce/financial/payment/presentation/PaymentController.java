package shop.dear.commerce.financial.payment.presentation;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shop.dear.commerce.financial.payment.application.PaymentService;
import shop.dear.commerce.financial.payment.application.dto.PaymentInfo;
import shop.dear.commerce.financial.payment.presentation.dto.request.PaymentRequest;
import shop.dear.commerce.financial.payment.presentation.dto.response.PaymentResponse;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;

import static shop.dear.common.response.ApiResponse.successWithData;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(

            @AuthUser
            final Long memberId,

            @Valid
            @RequestBody
            final PaymentRequest request
    ) {
        final PaymentInfo paymentInfo = paymentService.payOrder(
                request.toCommand(memberId)
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(successWithData(PaymentResponse.from(paymentInfo)));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @AuthUser
            final Long memberId,

            @Positive
            @PathVariable
            final Long paymentId
    ) {
        final PaymentInfo paymentInfo = paymentService.getPayment(
                memberId,
                paymentId
        );

        return ResponseEntity.ok(
                successWithData(PaymentResponse.from(paymentInfo))
        );
    }
}
