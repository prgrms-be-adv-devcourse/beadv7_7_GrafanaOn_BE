package shop.dear.commerce.financial.settlement.infrastructure.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.InboxMessageStatus;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.SettlementInbox;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.InboxMessageStatus;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.SettlementInbox;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.SettlementInboxProcessor;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.SettlementInboxService;
import shop.dear.common.messaging.consumer.StreamMessage;
import shop.dear.common.type.OrderType;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderFinishedStreamMessageHandlerTest {

    private static final Long INBOX_ID = 1L;
    private static final String EVENT_ID = "3f1a6d2e-0f5c-4a3b-9a1e-6c2f8b7d4e10";
    private static final String PAYLOAD = """
            {"orderId":11,"buyerId":2,"sellerId":3,"productId":4,"amount":10000.00,"orderType":"PURCHASE"}
            """;

    @Mock
    private SettlementInboxService settlementInboxService;

    @Mock
    private SettlementInboxProcessor settlementInboxProcessor;

    @Captor
    private ArgumentCaptor<SettlementInbox> inboxCaptor;

    private OrderFinishedStreamMessageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderFinishedStreamMessageHandler(
                settlementInboxService,
                settlementInboxProcessor,
                JsonMapper.builder().build()
        );
    }

    @Test
    void handle_savesInboxThenCreatesSettlement() {
        // given
        final StreamMessage message =
                new StreamMessage(EVENT_ID, "FinishedOrderEvent", PAYLOAD, 42L);
        given(settlementInboxService.save(any())).willReturn(Optional.of(INBOX_ID));

        // when
        handler.handle(message);

        // then
        verify(settlementInboxService).save(inboxCaptor.capture());

        final SettlementInbox inbox = inboxCaptor.getValue();
        assertThat(inbox.getEventId()).isEqualTo(EVENT_ID);
        assertThat(inbox.getEventType()).isEqualTo("FinishedOrderEvent");
        assertThat(inbox.getAggregateType()).isEqualTo(OrderType.PURCHASE.name());
        assertThat(inbox.getAggregateId()).isEqualTo(11L);
        assertThat(inbox.getStreamName())
                .isEqualTo(OrderFinishedStreamMessageHandler.ORDER_FINISHED_STREAM);
        assertThat(inbox.getStatus()).isEqualTo(InboxMessageStatus.PENDING);

        verify(settlementInboxProcessor).process(INBOX_ID);
    }

    @Test
    void handle_skipsSettlementWhenMessageIsDuplicate() {
        // given
        final StreamMessage message =
                new StreamMessage(EVENT_ID, "FinishedOrderEvent", PAYLOAD, 42L);
        given(settlementInboxService.save(any())).willReturn(Optional.empty());

        // when
        handler.handle(message);

        // then
        // 이미 적재된 메시지는 이미 처리도 끝났거나 FAILED 로 남아 있다.
        verify(settlementInboxProcessor, never()).process(anyLong());
    }

    @Test
    void handle_marksInboxFailedWhenSettlementFails() {
        // given
        final StreamMessage message =
                new StreamMessage(EVENT_ID, "FinishedOrderEvent", PAYLOAD, 42L);
        given(settlementInboxService.save(any())).willReturn(Optional.of(INBOX_ID));
        willThrow(new IllegalStateException("지갑 조회 실패"))
                .given(settlementInboxProcessor).process(INBOX_ID);

        // when & then
        // 예외를 밖으로 던지면 Consumer 가 이 메시지에서 무한 재시작한다.
        assertThatCode(() -> handler.handle(message)).doesNotThrowAnyException();

        verify(settlementInboxProcessor).markFailed(eq(INBOX_ID), eq("지갑 조회 실패"));
    }

    @Test
    void handle_savesFailedInboxWhenPayloadIsBroken() {
        // given
        final StreamMessage message =
                new StreamMessage(EVENT_ID, "FinishedOrderEvent", "not-a-json", 43L);

        // when
        handler.handle(message);

        // then
        verify(settlementInboxService).save(inboxCaptor.capture());

        final SettlementInbox inbox = inboxCaptor.getValue();
        assertThat(inbox.getStatus()).isEqualTo(InboxMessageStatus.FAILED);
        // jsonb 컬럼에 넣을 수 있도록 감싸되 원문은 보존한다.
        assertThat(inbox.getPayload()).isEqualTo("{\"raw\":\"not-a-json\"}");
        assertThat(inbox.getAggregateId()).isNull();

        verify(settlementInboxProcessor, never()).process(anyLong());
    }

    @Test
    void handle_skipsMessageOfUnknownEventType() {
        // given
        final StreamMessage message =
                new StreamMessage(EVENT_ID, "SomethingElseEvent", PAYLOAD, 44L);

        // when
        handler.handle(message);

        // then
        verify(settlementInboxService, never()).save(any());
    }

    @Test
    void handle_skipsMessageWithoutEventId() {
        // given
        final StreamMessage message =
                new StreamMessage(null, "FinishedOrderEvent", PAYLOAD, 45L);

        // when
        handler.handle(message);

        // then
        // eventId 가 없으면 중복 여부를 판단할 수 없으므로 적재하지 않는다.
        verify(settlementInboxService, never()).save(any());
    }
}
