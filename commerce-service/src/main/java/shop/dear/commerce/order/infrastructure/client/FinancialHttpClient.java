package shop.dear.commerce.order.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import shop.dear.common.event.financial.PaymentHoldRequestedEvent;
import shop.dear.common.event.financial.PaymentReleaseRequestedEvent;
import shop.dear.common.event.financial.PaymentRequestedEvent;
import shop.dear.common.exception.ServiceUnavailableException;
import shop.dear.commerce.order.common.exception.OrderErrorCode;
import shop.dear.commerce.order.infrastructure.client.config.FinancialCircuitBreakerConfig;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinancialHttpClient {

    private static final String PAYMENTS_ORDERS_URI = "/internal/payments/orders";
    private static final String WALLETS_HOLD_URI = "/internal/wallets/hold";
    private static final String WALLETS_RELEASE_URI = "/internal/wallets/release";

    private final RestClient financialRestClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public void requestPayment(final PaymentRequestedEvent event) {
        circuitBreakerFactory.create(FinancialCircuitBreakerConfig.FINANCIAL_PAYMENT).run(
                () -> {
                    financialRestClient.post()
                            .uri(PAYMENTS_ORDERS_URI)
                            .body(event)
                            .retrieve()
                            .toBodilessEntity();
                    return null;
                },
                this::fallback
        );
    }

    public void hold(final PaymentHoldRequestedEvent event) {
        circuitBreakerFactory.create(FinancialCircuitBreakerConfig.FINANCIAL_WALLET_HOLD).run(
                () -> {
                    financialRestClient.post()
                            .uri(WALLETS_HOLD_URI)
                            .body(event)
                            .retrieve()
                            .toBodilessEntity();
                    return null;
                },
                this::fallback
        );
    }

    public void release(final PaymentReleaseRequestedEvent event) {
        circuitBreakerFactory.create(FinancialCircuitBreakerConfig.FINANCIAL_WALLET_RELEASE).run(
                () -> {
                    financialRestClient.post()
                            .uri(WALLETS_RELEASE_URI)
                            .body(event)
                            .retrieve()
                            .toBodilessEntity();
                    return null;
                },
                this::fallback
        );
    }

    private Void fallback(final Throwable cause) {
        if (cause instanceof HttpClientErrorException) {
            log.error("Financial 서비스가 요청을 거부했습니다. 요청 형식을 확인해주세요.", cause);
            throw new IllegalStateException("Financial 서비스가 요청을 거부했습니다.", cause);
        }

        log.warn("Financial 서비스 호출 실패 -> fallback 실행. 원인: {}", cause.toString());
        throw new ServiceUnavailableException(OrderErrorCode.FINANCIAL_SERVICE_UNAVAILABLE);
    }
}
