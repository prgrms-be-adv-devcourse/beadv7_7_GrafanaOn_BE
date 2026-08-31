package shop.dear.commerce.financial.payment.infrastructure.client;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import shop.dear.commerce.financial.payment.infrastructure.outbox.PaymentOutbox;
import shop.dear.commerce.financial.payment.infrastructure.outbox.PaymentOutboxEventType;
import tools.jackson.databind.ObjectMapper;

class OrderPaymentResultClientTest {

    private MockRestServiceServer server;
    private OrderPaymentResultClient orderPaymentResultClient;

    @BeforeEach
    void setUp() {
        final RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("http://order-service");
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        orderPaymentResultClient = new OrderPaymentResultClient(
                restClientBuilder.build(),
                new ObjectMapper()
        );
    }

    @Test
    void sendsCompletedPaymentToOrderWithEventIdHeaderAndJsonBody() {
        final PaymentOutbox outbox = PaymentOutbox.of(
                1L,
                PaymentOutboxEventType.PAYMENT_COMPLETED,
                "{\"paymentId\":1,\"orderId\":100}"
        );
        server.expect(once(), requestTo(
                        "http://order-service/internal/orders/payment-completed"
                ))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Event-Id", outbox.getEventId()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(outbox.getPayload()))
                .andRespond(withSuccess());

        orderPaymentResultClient.send(outbox);

        server.verify();
    }

    @Test
    void sendsFailedPaymentToOrderWithEventIdHeaderAndJsonBody() {
        final PaymentOutbox outbox = PaymentOutbox.of(
                1L,
                PaymentOutboxEventType.PAYMENT_FAILED,
                "{\"paymentId\":1,\"orderId\":100}"
        );
        server.expect(once(), requestTo(
                        "http://order-service/internal/orders/payment-failed"
                ))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Event-Id", outbox.getEventId()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(outbox.getPayload()))
                .andRespond(withSuccess());

        orderPaymentResultClient.send(outbox);

        server.verify();
    }
}
