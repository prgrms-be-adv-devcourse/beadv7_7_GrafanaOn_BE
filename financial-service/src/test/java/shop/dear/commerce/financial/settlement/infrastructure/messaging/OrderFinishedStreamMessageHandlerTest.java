package shop.dear.commerce.financial.settlement.infrastructure.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.InboxMessageStatus;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.InboxSaveResult;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.SettlementInbox;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.SettlementInboxProcessor;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.SettlementInboxService;
import shop.dear.common.messaging.consumer.StreamMessage;
import shop.dear.common.type.OrderType;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        given(settlementInboxService.save(any()))
                .willReturn(new InboxSaveResult(INBOX_ID, InboxMessageStatus.PENDING));

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
    void handle_retriesSettlementWhenDuplicateInboxIsStillPending() {
        // given
        // 적재 직후 장애로 정산까지 가지 못한 메시지는 재수신 시 기존 PENDING 행을 돌려받는다.
        final StreamMessage message =
                new StreamMessage(EVENT_ID, "FinishedOrderEvent", PAYLOAD, 42L);
        given(settlementInboxService.save(any()))
                .willReturn(new InboxSaveResult(INBOX_ID, InboxMessageStatus.PENDING));

        // when
        handler.handle(message);

        // then
        // 중복이라는 이유로 건너뛰지 않고 남은 정산 처리를 이어서 수행한다.
        verify(settlementInboxProcessor).process(INBOX_ID);
    }

    @Test
    void handle_skipsSettlementWhenInboxIsAlreadyProcessed() {
        // given
        final StreamMessage message =
                new StreamMessage(EVENT_ID, "FinishedOrderEvent", PAYLOAD, 42L);
        given(settlementInboxService.save(any()))
                .willReturn(new InboxSaveResult(INBOX_ID, InboxMessageStatus.PROCESSED));

        // when
        handler.handle(message);

        // then
        verify(settlementInboxProcessor, never()).process(anyLong());
    }

    @Test
    void handle_skipsSettlementWhenInboxIsAlreadyFailed() {
        // given
        final StreamMessage message =
                new StreamMessage(EVENT_ID, "FinishedOrderEvent", PAYLOAD, 42L);
        given(settlementInboxService.save(any()))
                .willReturn(new InboxSaveResult(INBOX_ID, InboxMessageStatus.FAILED));

        // when
        handler.handle(message);

        // then
        // FAILED 재시도는 retryCount 와 backoff 를 관리하는 재처리 주체가 담당한다.
        verify(settlementInboxProcessor, never()).process(anyLong());
    }

    @Test
    void handle_marksInboxFailedWhenSettlementFails() {
        // given
        final StreamMessage message =
                new StreamMessage(EVENT_ID, "FinishedOrderEvent", PAYLOAD, 42L);
        given(settlementInboxService.save(any()))
                .willReturn(new InboxSaveResult(INBOX_ID, InboxMessageStatus.PENDING));
        willThrow(new IllegalStateException("지갑 조회 실패"))
                .given(settlementInboxProcessor).process(INBOX_ID);

        // when & then
        // FAILED 로 남기는 데 성공했다면 재수신해도 건너뛰므로 예외를 던지지 않는다.
        assertThatCode(() -> handler.handle(message)).doesNotThrowAnyException();

        verify(settlementInboxProcessor).markFailed(eq(INBOX_ID), eq("지갑 조회 실패"));
    }

    @Test
    void handle_propagatesWhenMarkingFailedAlsoFails() {
        // given
        final StreamMessage message =
                new StreamMessage(EVENT_ID, "FinishedOrderEvent", PAYLOAD, 42L);
        given(settlementInboxService.save(any()))
                .willReturn(new InboxSaveResult(INBOX_ID, InboxMessageStatus.PENDING));
        willThrow(new IllegalStateException("지갑 조회 실패"))
                .given(settlementInboxProcessor).process(INBOX_ID);
        willThrow(new IllegalStateException("DB 연결 실패"))
                .given(settlementInboxProcessor).markFailed(eq(INBOX_ID), any());

        // when & then
        // FAILED 로도 남기지 못하면 inbox 가 PENDING 으로 남는다.
        // 이때 정상 반환하면 offset 이 저장되어 메시지가 다시 오지 않으므로 예외를 전파한다.
        assertThatThrownBy(() -> handler.handle(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FAILED 로 기록하지 못했습니다");
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
