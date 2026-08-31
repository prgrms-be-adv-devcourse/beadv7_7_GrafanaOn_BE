package shop.dear.commerce.product.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.product.application.ProductService;
import shop.dear.commerce.product.infrastructure.inbox.ProductInboxRepository;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.messaging.consumer.StreamMessage;
import shop.dear.common.messaging.consumer.StreamMessageHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinishedOrderEventMessageHandler implements StreamMessageHandler {

    private final ProductInboxRepository inboxRepository;
    private final ObjectMapper objectMapper;
    private final ProductService productService;

    @Override
    @Transactional
    public void handle(final StreamMessage message) {
        if (!FinishedOrderStream.EVENT_TYPE.equals(message.eventType())) {
            log.warn(
                    "[FinishedOrderEventMessageHandler] 알 수 없는 eventType 무시. eventType={}, eventId={}",
                    message.eventType(),
                    message.eventId()
            );
            return;
        }

        final int insertedCount = inboxRepository.insertIfAbsent(
                FinishedOrderStream.CONSUMER_NAME,
                message.eventId(),
                message.eventType(),
                message.payload()
        );

        if (insertedCount == 0) {
            log.info(
                    "[FinishedOrderEventMessageHandler] 중복 이벤트 스킵. eventId={}",
                    message.eventId()
            );
            return;
        }

        final FinishedOrderEvent event = deserialize(message);

        try {
            productService.completeProductSale(event.productId());
        } catch (final RuntimeException exception) {
            log.error(
                    "주문 완료 후 상품 상태 변경 실패 - productId: {}, eventId={}",
                    event.productId(),
                    message.eventId(),
                    exception
            );
            throw exception;
        }
    }

    private FinishedOrderEvent deserialize(final StreamMessage message) {
        try {
            return objectMapper.readValue(message.payload(), FinishedOrderEvent.class);
        } catch (final JacksonException exception) {
            throw new IllegalArgumentException(
                    "FinishedOrderEvent 역직렬화에 실패했습니다. eventId=" + message.eventId(),
                    exception
            );
        }
    }
}
