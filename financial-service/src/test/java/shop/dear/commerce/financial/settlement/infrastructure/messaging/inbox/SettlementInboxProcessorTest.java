package shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.financial.settlement.application.SettlementService;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.InboxMessageStatus;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.SettlementInbox;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.SettlementInboxJpaRepository;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.OrderFinishedStreamMessageHandler;
import shop.dear.common.type.OrderType;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettlementInboxProcessorTest {

    private static final Long INBOX_ID = 1L;
    private static final String EVENT_ID = "3f1a6d2e-0f5c-4a3b-9a1e-6c2f8b7d4e10";
    private static final String PAYLOAD = """
            {"orderId":11,"buyerId":2,"sellerId":3,"productId":4,"amount":10000.00,"orderType":"PURCHASE"}
            """;

    @Mock
    private SettlementInboxJpaRepository settlementInboxJpaRepository;

    @Mock
    private SettlementService settlementService;

    @Captor
    private ArgumentCaptor<FinishedOrderEvent> eventCaptor;

    private SettlementInboxProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new SettlementInboxProcessor(
                settlementInboxJpaRepository,
                settlementService,
                JsonMapper.builder().build()
        );
    }

    private SettlementInbox pendingInbox() {
        return SettlementInbox.pending(
                EVENT_ID,
                InboxMessageEventType.ORDER_FINISHED.publishedName(),
                OrderType.PURCHASE.name(),
                11L,
                OrderFinishedStreamMessageHandler.ORDER_FINISHED_STREAM,
                PAYLOAD
        );
    }

    @Test
    void process_createsSettlementAndMarksProcessed() {
        // given
        final SettlementInbox inbox = pendingInbox();
        given(settlementInboxJpaRepository.findById(INBOX_ID)).willReturn(Optional.of(inbox));

        // when
        processor.process(INBOX_ID);

        // then
        verify(settlementService).createSettlement(eventCaptor.capture());

        final FinishedOrderEvent event = eventCaptor.getValue();
        assertThat(event.orderId()).isEqualTo(11L);
        assertThat(event.sellerId()).isEqualTo(3L);
        assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(event.orderType()).isEqualTo(OrderType.PURCHASE.name());

        assertThat(inbox.getStatus()).isEqualTo(InboxMessageStatus.PROCESSED);
        assertThat(inbox.getCompletedAt()).isNotNull();
    }

    @Test
    void process_skipsInboxAlreadyProcessed() {
        // given
        final SettlementInbox inbox = pendingInbox();
        inbox.markAsProcessed();
        given(settlementInboxJpaRepository.findById(INBOX_ID)).willReturn(Optional.of(inbox));

        // when
        processor.process(INBOX_ID);

        // then
        // 재전송된 메시지로 같은 주문을 두 번 정산하지 않는다.
        verify(settlementService, never()).createSettlement(any());
    }

    @Test
    void process_throwsWhenSettlementFailsSoTransactionRollsBack() {
        // given
        final SettlementInbox inbox = pendingInbox();
        given(settlementInboxJpaRepository.findById(INBOX_ID)).willReturn(Optional.of(inbox));
        willThrow(new IllegalStateException("지갑 조회 실패"))
                .given(settlementService).createSettlement(any());

        // when & then
        assertThatThrownBy(() -> processor.process(INBOX_ID))
                .isInstanceOf(IllegalStateException.class);

        // 정산 생성이 실패했으므로 PROCESSED 로 바뀌지 않는다.
        assertThat(inbox.getStatus()).isEqualTo(InboxMessageStatus.PENDING);
        assertThat(inbox.getCompletedAt()).isNull();
    }

    @Test
    void markFailed_marksInboxFailed() {
        // given
        final SettlementInbox inbox = pendingInbox();
        given(settlementInboxJpaRepository.findById(INBOX_ID)).willReturn(Optional.of(inbox));

        // when
        processor.markFailed(INBOX_ID, "지갑 조회 실패");

        // then
        assertThat(inbox.getStatus()).isEqualTo(InboxMessageStatus.FAILED);
        assertThat(inbox.getRetryCount()).isEqualTo(1);
    }
}
