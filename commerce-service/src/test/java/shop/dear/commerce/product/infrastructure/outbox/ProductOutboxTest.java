package shop.dear.commerce.product.infrastructure.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductOutboxTest {

    private ProductOutbox outbox;

    @BeforeEach
    void setUp() {
        outbox = ProductOutbox.of("Product", "1", ProductOutboxEvent.PRODUCT_UPDATED, "{\"productId\":1}");
    }

    @Test
    @DisplayName("적재된 직후에는 PENDING이고 재시도 횟수는 0이다")
    void createdAsPending() {
        assertEquals(ProductOutboxStatus.PENDING, outbox.getStatus());
        assertEquals(0, outbox.getRetryCount());
        assertNull(outbox.getSentAt());
        assertNull(outbox.getLastError());
    }

    @Test
    @DisplayName("전달에 성공하면 SENT가 되고 전송 시각이 기록된다")
    void markSent() {
        outbox.markSent();

        assertEquals(ProductOutboxStatus.SENT, outbox.getStatus());
        assertNotNull(outbox.getSentAt());
    }

    @Test
    @DisplayName("전달에 실패하면 FAILED가 되고 재시도 횟수와 실패 사유가 남는다")
    void markFailed() {
        outbox.markFailed("connection refused");

        assertEquals(ProductOutboxStatus.FAILED, outbox.getStatus());
        assertEquals(1, outbox.getRetryCount());
        assertEquals("connection refused", outbox.getLastError());
    }

    @Test
    @DisplayName("실패가 반복되면 재시도 횟수만 누적되고 상태는 FAILED로 유지된다")
    void markFailedRepeatedly() {
        outbox.markFailed("1차 실패");
        outbox.markFailed("2차 실패");
        outbox.markFailed("3차 실패");

        assertEquals(ProductOutboxStatus.FAILED, outbox.getStatus());
        assertEquals(3, outbox.getRetryCount());
        assertEquals("3차 실패", outbox.getLastError());
    }

    @Test
    @DisplayName("실패했던 이벤트도 이후 전달에 성공하면 SENT가 된다")
    void failedThenSent() {
        outbox.markFailed("일시적 장애");

        outbox.markSent();

        assertEquals(ProductOutboxStatus.SENT, outbox.getStatus());
        assertNotNull(outbox.getSentAt());
        // 실패 이력은 남겨 둔다
        assertEquals(1, outbox.getRetryCount());
        assertEquals("일시적 장애", outbox.getLastError());
    }
}
