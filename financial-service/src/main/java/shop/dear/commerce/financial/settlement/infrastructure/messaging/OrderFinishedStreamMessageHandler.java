package shop.dear.commerce.financial.settlement.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.InboxMessageEventType;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.InboxSaveResult;
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

        final InboxSaveResult saved = settlementInboxService.save(SettlementInbox.pending(
                message.eventId(),
                message.eventType(),
                event.orderType(),
                event.orderId(),
                ORDER_FINISHED_STREAM,
                message.payload()
        ));

        // 적재 직후 장애로 정산까지 가지 못한 메시지는 재수신 시 PENDING 으로 남아 있다.
        // 아직 처리되지 않았다면 이어서 정산을 시도한다.
        if (!saved.isPending()) {
            log.info(
                    "[SettlementInbox] 처리가 끝난 이벤트를 건너뜁니다. eventId={}, status={}, offset={}",
                    message.eventId(),
                    saved.status(),
                    message.offset()
            );
            return;
        }

        createSettlement(saved.id(), message);
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

    /**
     * 정산 실패를 inbox 에 FAILED 로 기록합니다.
     *
     * <p>기록까지 실패하면 inbox 는 PENDING 으로 남는다.
     * 이때 정상 반환하면 Listener 가 offset 을 저장해 메시지가 다시 오지 않으므로,
     * 정산도 없고 실패 흔적도 없는 상태로 메시지만 소비된다.
     * 그래서 예외를 전파해 offset 저장을 막고, 재수신 시 PENDING 을 다시 처리하게 한다.</p>
     */
    private void markFailed(
            final Long inboxId,
            final StreamMessage message,
            final Exception cause
    ) {
        try {
            settlementInboxProcessor.markFailed(inboxId, cause.getMessage());
        } catch (final Exception exception) {
            // 원래의 정산 실패 원인도 함께 남긴다.
            exception.addSuppressed(cause);

            log.error(
                    "[SettlementInbox] 실패 상태 기록에 실패해 offset 저장을 중단합니다. "
                            + "id={}, eventId={}, offset={}",
                    inboxId,
                    message.eventId(),
                    message.offset(),
                    exception
            );

            throw new IllegalStateException(
                    "정산 실패를 FAILED 로 기록하지 못했습니다. inboxId=" + inboxId,
                    exception
            );
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

            markFailed(inboxId, message, exception);
        }
    }
}
