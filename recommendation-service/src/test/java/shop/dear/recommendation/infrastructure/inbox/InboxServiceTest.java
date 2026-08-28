package shop.dear.recommendation.infrastructure.inbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
class InboxServiceTest {

    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 8, 25, 12, 0, 0);

    @Autowired
    private InboxService inboxService;

    @Autowired
    private RecommendationInboxJpaRepository recommendationInboxJpaRepository;

    private RecommendationInbox inbox(final Long eventId) {
        return RecommendationInbox.of(
            eventId,
            "Product",
            String.valueOf(eventId),
            InboxEventType.PRODUCT_UPDATED,
            "{\"productId\":" + eventId + "}",
            OCCURRED_AT
        );
    }

    @Test
    @DisplayName("새로운 이벤트는 모두 적재된다")
    void savesNewEvents() {
        inboxService.saveProductEvents(List.of(inbox(1L), inbox(2L)));

        assertThat(recommendationInboxJpaRepository.findAll())
            .extracting(RecommendationInbox::getEventId)
            .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("이미 적재된 eventId는 건너뛴다 (발신 측 재전송 대비)")
    void skipsAlreadySavedEvents() {
        inboxService.saveProductEvents(List.of(inbox(1L), inbox(2L)));

        // 발신 측이 같은 배치를 다시 보낸 상황
        inboxService.saveProductEvents(List.of(inbox(1L), inbox(2L), inbox(3L)));

        assertThat(recommendationInboxJpaRepository.findAll())
            .extracting(RecommendationInbox::getEventId)
            .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("같은 배치를 그대로 재전송하면 아무것도 추가되지 않는다")
    void savesNothingWhenAllDuplicated() {
        inboxService.saveProductEvents(List.of(inbox(1L), inbox(2L)));

        inboxService.saveProductEvents(List.of(inbox(1L), inbox(2L)));

        assertThat(recommendationInboxJpaRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("한 배치 안에 같은 eventId가 두 번 들어와도 한 건만 적재된다")
    void savesOnceWhenBatchHasDuplicates() {
        inboxService.saveProductEvents(List.of(inbox(1L), inbox(1L), inbox(2L)));

        assertThat(recommendationInboxJpaRepository.findAll())
            .extracting(RecommendationInbox::getEventId)
            .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("빈 배치를 받아도 예외 없이 넘어간다")
    void acceptsEmptyBatch() {
        inboxService.saveProductEvents(List.of());

        assertThat(recommendationInboxJpaRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("적재된 이벤트는 PENDING 상태이고 payload가 그대로 보존된다")
    void savedAsPendingWithPayload() {
        inboxService.saveProductEvents(List.of(inbox(1L)));

        final RecommendationInbox saved = recommendationInboxJpaRepository.findAll().get(0);

        assertThat(saved.getStatus()).isEqualTo(InboxStatus.PENDING);
        assertThat(saved.getEventType()).isEqualTo(InboxEventType.PRODUCT_UPDATED);
        assertThat(saved.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(saved.getPayload()).contains("\"productId\"");
    }
}
