package shop.dear.commerce.financial.settlement.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.InboxMessageEventType;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.SettlementInbox;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.SettlementInboxProcessor;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.SettlementInboxService;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.messaging.consumer.StreamMessage;
import shop.dear.common.messaging.consumer.StreamMessageHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFinishedStreamMessageHandler implements StreamMessageHandler {


    public static final String ORDER_FINISHED_STREAM = "order.finished";

    private final SettlementInboxService settlementInboxService;
    private final SettlementInboxProcessor settlementInboxProcessor;
    private final ObjectMapper objectMapper;

    @Override
    public void handle(final StreamMessage message) {

        if (!InboxMessageEventType.ORDER_FINISHED.matches(message.eventType())) {
            log.warn(
                    "[SettlementInbox] 처리 대상이 아닌 이벤트를 건너뜁니다. eventType={}, offset={}",
                    message.eventType(),
                    message.offset()
            );
            return;
        }

        if (message.eventId() == null || message.eventId().isBlank()) {
            log.error(
                    "[SettlementInbox] eventId 가 없는 메시지를 건너뜁니다. eventType={}, offset={}",
                    message.eventType(),
                    message.offset()
            );
            return;
        }

        final FinishedOrderEvent event = parse(message);

        if (event == null) {
            settlementInboxService.save(SettlementInbox.failed(
                    message.eventId(),
                    message.eventType(),
                    ORDER_FINISHED_STREAM,
                    toRawEnvelope(message.payload()),
                    "payload 를 JSON 으로 해석하지 못했습니다."
            ));
            return;
        }

        settlementInboxService.save(SettlementInbox.pending(
                message.eventId(),
                message.eventType(),
                event.orderType(),
                event.orderId(),
                ORDER_FINISHED_STREAM,
                message.payload()
        )).ifPresent(inboxId -> createSettlement(inboxId, message));
    }

    private String toRawEnvelope(final String payload) {
        return objectMapper.writeValueAsString(Map.of("raw", payload == null ? "" : payload));
    }

    private FinishedOrderEvent parse(final StreamMessage message) {
        try {
            return objectMapper.readValue(message.payload(), FinishedOrderEvent.class);
        } catch (final JacksonException exception) {
            log.error(
                    "[SettlementInbox] payload 를 해석하지 못했습니다. eventId={}, offset={}",
                    message.eventId(),
                    message.offset(),
                    exception
            );

            return null;
        }
    }

    private void markFailed(final Long inboxId, final String reason) {
        try {
            settlementInboxProcessor.markFailed(inboxId, reason);
        } catch (final Exception exception) {
            log.error("[SettlementInbox] 실패 상태 기록에 실패했습니다. id={}", inboxId, exception);
        }
    }

    private void createSettlement(final Long inboxId, final StreamMessage message) {
        try {
            settlementInboxProcessor.process(inboxId);
        } catch (final Exception exception) {
            log.error(
                    "[SettlementInbox] 정산 건 생성에 실패했습니다. id={}, eventId={}, offset={}",
                    inboxId,
                    message.eventId(),
                    message.offset(),
                    exception
            );

            markFailed(inboxId, exception.getMessage());
        }
    }
}
