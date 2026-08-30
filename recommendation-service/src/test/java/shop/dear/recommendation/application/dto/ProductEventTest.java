package shop.dear.recommendation.application.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductEventTest {

    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 8, 25, 12, 0, 0);

    private ProductEvent event;

    @BeforeEach
    void setUp() {
        event = new ProductEvent(1L, "Product", "100", "PRODUCT_UPDATED", "{\"productId\":100}", OCCURRED_AT);
    }

    @Test
    @DisplayName("마지막 처리 시각보다 이전에 발생한 이벤트는 오래된 것으로 판단한다")
    void staleWhenOlderThanLastProcessed() {
        assertTrue(event.isStaleAgainst(OCCURRED_AT.plusSeconds(1)));
    }

    @Test
    @DisplayName("마지막 처리 시각과 같거나 이후에 발생한 이벤트는 오래된 것이 아니다")
    void notStaleWhenSameOrNewer() {
        assertFalse(event.isStaleAgainst(OCCURRED_AT));
        assertFalse(event.isStaleAgainst(OCCURRED_AT.minusSeconds(1)));
    }

    @Test
    @DisplayName("처리 이력이 없으면(null) 오래된 것이 아니다")
    void notStaleWhenNeverProcessed() {
        assertFalse(event.isStaleAgainst(null));
    }

    @Test
    @DisplayName("삭제 이벤트를 구분한다")
    void distinguishesDeleteEvent() {
        assertFalse(event.isDeleted());
        assertTrue(
            new ProductEvent(1L, "Product", "100", "PRODUCT_DELETED", "{}", OCCURRED_AT).isDeleted()
        );
    }
}
