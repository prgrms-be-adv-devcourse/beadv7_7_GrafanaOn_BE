package shop.dear.commerce.financial.payment.infrastructure.client.toss;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import shop.dear.commerce.financial.payment.application.dto.PgApprovalResult;
import shop.dear.commerce.financial.payment.application.port.PgPaymentApprovalPort;
import shop.dear.commerce.financial.payment.domain.exception.PaymentErrorCode;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class TossPaymentApprovalAdapter implements PgPaymentApprovalPort {

    private final RestClient restClient;
    private final String secretKey;

    public TossPaymentApprovalAdapter(
            @Qualifier("tossPaymentRestClient")
            final RestClient restClient,

            @Value("${toss.secret-key:}")
            final String secretKey
    ) {
        this.restClient = restClient;
        this.secretKey = secretKey;
    }

    @Override
    public PgApprovalResult approve(
            final String paymentKey,
            final String orderId,
            final BigDecimal amount,
            final String idempotencyKey
    ) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new BusinessException(PaymentErrorCode.PG_APPROVAL_FAILED);
        }

        try {
            final TossConfirmResponse response = restClient.post()
                    .uri("/v1/payments/confirm")
                    .header(HttpHeaders.AUTHORIZATION, basicAuthorization())
                    .header("Idempotency-Key", idempotencyKey)
                    .body(new TossConfirmRequest(paymentKey, orderId, amount))
                    .retrieve()
                    .body(TossConfirmResponse.class);

            if (response == null) {
                throw new BusinessException(
                        PaymentErrorCode.PG_APPROVAL_FAILED
                );
            }

            return new PgApprovalResult(
                    response.orderId(),
                    response.transactionKey(),
                    response.totalAmount()
            );
        } catch (final RestClientException e) {
            throw new BusinessException(
                    PaymentErrorCode.PG_APPROVAL_FAILED
            );
        }
    }

    private String basicAuthorization() {
        final String credentials = secretKey + ":";

        return "Basic " + Base64.getEncoder()
                .encodeToString(
                        credentials.getBytes(StandardCharsets.UTF_8)
                );
    }

    private record TossConfirmRequest(
            String paymentKey,
            String orderId,
            BigDecimal amount
    ) {
    }

    private record TossConfirmResponse(
            String orderId,
            String transactionKey,
            BigDecimal totalAmount
    ) {
    }
}
