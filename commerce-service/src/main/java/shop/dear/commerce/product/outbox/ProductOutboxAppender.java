package shop.dear.commerce.product.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductOutboxAppender {

    private static final String AGGREGATE_TYPE = "Product";

    private final ProductOutboxRepository productOutboxRepository;
    private final ObjectMapper objectMapper;

    public void append(final Long productId, final ProductOutboxEvent eventType, final String story) {
        final ProductOutboxPayload payload = ProductOutboxPayload.of(productId, story);

        String payloadJson = objectMapper.writeValueAsString(payload);

        productOutboxRepository.save(
            ProductOutbox.of(AGGREGATE_TYPE, String.valueOf(productId), eventType, payloadJson)
        );

        log.info("[ProductOutbox] outbox 적재. eventType={}, productId={}", eventType, productId);
    }
}
