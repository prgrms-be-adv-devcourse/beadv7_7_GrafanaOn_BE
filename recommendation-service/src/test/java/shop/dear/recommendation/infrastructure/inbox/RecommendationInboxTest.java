package shop.dear.recommendation.infrastructure.inbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationInboxTest {

    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 8, 25, 12, 0, 0);

    private RecommendationInbox inbox;

    @BeforeEach
    void setUp() {
        inbox = RecommendationInbox.of(
            1L,
            "Product",
            "100",
            InboxEventType.PRODUCT_UPDATED,
            "{\"productId\":100}",
            OCCURRED_AT
        );
    }

    @Test
    @DisplayName("적재된 직후에는 PENDING이다")
    void createdAsPending() {
        assertEquals(InboxStatus.PENDING, inbox.getStatus());
        assertTrue(inbox.isPending());
        assertEquals(0, inbox.getRetryCount());
    }

    @Test
    @DisplayName("처리에 성공하면 PROCESSED가 된다")
    void markAsProcessed() {
        inbox.markAsProcessed();

        assertEquals(InboxStatus.PROCESSED, inbox.getStatus());
        assertFalse(inbox.isPending());
    }

    @Test
    @DisplayName("처리에 실패하면 FAILED가 되고 사유와 시도 횟수가 남는다")
    void markAsFailed() {
        inbox.markAsFailed("임베딩 실패");

        assertEquals(InboxStatus.FAILED, inbox.getStatus());
        assertFalse(inbox.isPending());
        assertEquals("임베딩 실패", inbox.getLastError());
        assertEquals(1, inbox.getRetryCount());
    }

    @Test
    @DisplayName("재시도할 때마다 시도 횟수가 쌓이고 마지막 사유로 갱신된다")
    void markAsFailedAccumulatesRetryCount() {
        inbox.markAsFailed("모델 서버 연결 실패");
        inbox.markAsFailed("타임아웃");

        assertEquals(InboxStatus.FAILED, inbox.getStatus());
        assertEquals("타임아웃", inbox.getLastError());
        assertEquals(2, inbox.getRetryCount());
    }
}
