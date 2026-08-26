package shop.dear.recommendation.infrastructure.inbox;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InboxServiceConcurrencyTest {

    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 8, 25, 12, 0, 0);
    private static final int THREAD_COUNT = 10;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private RecommendationInboxJpaRepository recommendationInboxJpaRepository;

    @AfterEach
    void tearDown() {
        recommendationInboxJpaRepository.deleteAllInBatch();
    }

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
    @DisplayName("같은 배치가 동시에 도착해도 예외 없이 한 건씩만 적재된다")
    void savesOnceWhenSameBatchArrivesConcurrently() throws InterruptedException {
        final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        final CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        final CountDownLatch done = new CountDownLatch(THREAD_COUNT);
        final AtomicInteger failed = new AtomicInteger();

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    // 모든 스레드가 동시에 출발하도록 맞춘다
                    ready.countDown();
                    ready.await();

                    inboxService.saveProductEvents(List.of(inbox(1L), inbox(2L)));
                } catch (final Exception e) {
                    failed.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        done.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(failed.get()).isZero();
        assertThat(recommendationInboxJpaRepository.findAll())
            .extracting(RecommendationInbox::getEventId)
            .containsExactlyInAnyOrder(1L, 2L);
    }
}
