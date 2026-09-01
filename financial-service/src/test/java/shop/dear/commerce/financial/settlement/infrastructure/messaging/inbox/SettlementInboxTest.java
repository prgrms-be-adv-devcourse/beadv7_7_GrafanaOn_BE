package shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shop.dear.common.type.OrderType;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementInboxTest {

    private static final String EVENT_ID = "3f1a6d2e-0f5c-4a3b-9a1e-6c2f8b7d4e10";
    private static final String EVENT_TYPE = "FinishedOrderEvent";
    private static final String STREAM_NAME = "order.finished";
    private static final String PAYLOAD = "{\"orderId\":11}";

    private SettlementInbox inbox;

    @BeforeEach
    void setUp() {
        inbox = SettlementInbox.pending(
            EVENT_ID,
            EVENT_TYPE,
            OrderType.PURCHASE.name(),
            11L,
            STREAM_NAME,
            PAYLOAD
        );
    }

    @Test
    @DisplayName("적재된 메시지는 처리 대기 상태로 시작한다")
    void pending_startsAsPendingWithoutError() {
        assertThat(inbox.isPending()).isTrue();
        assertThat(inbox.getStatus()).isEqualTo(InboxMessageStatus.PENDING);
        assertThat(inbox.getRetryCount()).isZero();
        assertThat(inbox.getLastError()).isNull();
        assertThat(inbox.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("처리에 성공하면 완료 시각을 남긴다")
    void markAsProcessed_recordsCompletedAt() {
        inbox.markAsProcessed();

        assertThat(inbox.isPending()).isFalse();
        assertThat(inbox.getStatus()).isEqualTo(InboxMessageStatus.PROCESSED);
        assertThat(inbox.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("처리에 실패하면 사유와 시도 횟수를 남긴다")
    void markAsFailed_recordsReasonAndRetryCount() {
        inbox.markAsFailed("지갑 조회 실패");

        assertThat(inbox.getStatus()).isEqualTo(InboxMessageStatus.FAILED);
        assertThat(inbox.getRetryCount()).isEqualTo(1);
        assertThat(inbox.getLastError()).isEqualTo("지갑 조회 실패");
        assertThat(inbox.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("payload 를 해석하지 못한 메시지는 사유와 함께 실패로 적재된다")
    void failed_keepsPayloadAndReason() {
        final SettlementInbox broken = SettlementInbox.failed(
            EVENT_ID,
            EVENT_TYPE,
            STREAM_NAME,
            "{\"raw\":\"not-a-json\"}",
            "payload 를 JSON 으로 해석하지 못했습니다."
        );

        assertThat(broken.getStatus()).isEqualTo(InboxMessageStatus.FAILED);
        assertThat(broken.getAggregateType()).isNull();
        assertThat(broken.getAggregateId()).isNull();
        assertThat(broken.getPayload()).isEqualTo("{\"raw\":\"not-a-json\"}");
        assertThat(broken.getLastError()).isEqualTo("payload 를 JSON 으로 해석하지 못했습니다.");
    }
}
