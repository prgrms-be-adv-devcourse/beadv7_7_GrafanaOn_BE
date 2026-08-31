package shop.dear.commerce.financial.payment.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import shop.dear.commerce.financial.payment.infrastructure.outbox.PaymentOutbox;
import shop.dear.commerce.financial.payment.infrastructure.outbox.PaymentOutboxEventType;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class OrderPaymentResultClient {

    private static final String EVENT_ID_HEADER = "X-Event-Id";
    private static final String PAYMENT_COMPLETED_URI =
            "/internal/orders/payment-completed";
    private static final String PAYMENT_FAILED_URI =
            "/internal/orders/payment-failed";

    private final RestClient orderPaymentResultRestClient;
    private final ObjectMapper objectMapper;

    /**
     * Outbox에 저장된 결제 결과를 Order 서비스로 전달한다.
     *
     * <p>eventId는 HTTP 헤더로 전달한다. HTTP 성공 후 Financial의 SENT 저장 전에
     * 장애가 발생하면 동일 Outbox가 재전송될 수 있으므로, Order는 이 값을 Inbox 멱등성 키로 사용해야 한다.</p>
     */
    public void send(final PaymentOutbox outbox) {
        orderPaymentResultRestClient.post()
                .uri(resolveUri(outbox.getEventType()))
                .header(EVENT_ID_HEADER, outbox.getEventId())
                .contentType(MediaType.APPLICATION_JSON)
                // 저장된 JSON 문자열을 다시 JSON 트리로 읽어 이중 직렬화를 막는다.
                .body(objectMapper.readTree(outbox.getPayload()))
                .retrieve()
                .toBodilessEntity();
    }

    private String resolveUri(final PaymentOutboxEventType eventType) {
        return switch (eventType) {
            case PAYMENT_COMPLETED -> PAYMENT_COMPLETED_URI;
            case PAYMENT_FAILED -> PAYMENT_FAILED_URI;
        };
    }
}
