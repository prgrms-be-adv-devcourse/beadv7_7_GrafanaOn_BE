package shop.dear.commerce.product.infrastructure.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.product.infrastructure.client.RecommendationHttpClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/**
 * 자동 폴링이 테스트 데이터와 겹치지 않도록 주기를 길게 잡고 스케줄 메서드를 직접 호출합니다.
 */
@Transactional
@SpringBootTest(properties = "product.outbox.polling-interval-ms=3600000")
class OutboxPollingSchedulerTest {

    @Autowired
    private OutboxPollingScheduler outboxPollingScheduler;

    @Autowired
    private ProductOutboxRepository productOutboxRepository;

    @MockitoBean
    private RecommendationHttpClient recommendationHttpClient;

    private ProductOutbox saveOutbox(final String aggregateId) {
        return productOutboxRepository.save(ProductOutbox.of(
            "Product",
            aggregateId,
            ProductOutboxEvent.PRODUCT_UPDATED,
            "{\"productId\":" + aggregateId + ",\"story\":\"이야기\"}"
        ));
    }

    @Test
    @DisplayName("전달에 성공하면 배치 전체가 SENT로 바뀐다")
    void marksSentOnSuccess() {
        saveOutbox("1");
        saveOutbox("2");

        outboxPollingScheduler.sendPendingEvents();

        assertThat(productOutboxRepository.findAll())
            .extracting(ProductOutbox::getStatus)
            .containsOnly(ProductOutboxStatus.SENT);

        then(recommendationHttpClient).should().sendProductEvents(anyList());
    }

    @Test
    @DisplayName("전달에 실패하면 FAILED가 되고 재시도 횟수와 실패 사유가 기록된다")
    void marksFailedOnError() {
        saveOutbox("1");

        willThrow(new RuntimeException("recommendation 응답 없음"))
            .given(recommendationHttpClient).sendProductEvents(anyList());

        outboxPollingScheduler.sendPendingEvents();

        final ProductOutbox failed = productOutboxRepository.findAll().get(0);

        assertThat(failed.getStatus()).isEqualTo(ProductOutboxStatus.FAILED);
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(failed.getLastError()).contains("recommendation 응답 없음");
    }

    @Test
    @DisplayName("실패했던 이벤트도 다음 주기에 다시 집어 전달한다")
    void retriesFailedEvent() {
        final ProductOutbox outbox = saveOutbox("1");
        outbox.markFailed("직전 시도 실패");

        outboxPollingScheduler.sendPendingEvents();

        assertThat(productOutboxRepository.findAll().get(0).getStatus())
            .isEqualTo(ProductOutboxStatus.SENT);
    }

    @Test
    @DisplayName("보낼 것이 없으면 전달을 시도하지 않는다")
    void doesNotSendWhenNothingPending() {
        final ProductOutbox sent = saveOutbox("1");
        sent.markSent();

        outboxPollingScheduler.sendPendingEvents();

        then(recommendationHttpClient).should(org.mockito.Mockito.never())
            .sendProductEvents(anyList());
    }

    @Test
    @DisplayName("전달 대상에는 SENT가 아닌 이벤트만 담긴다")
    void sendsOnlyUnsentEvents() {
        saveOutbox("1");

        final ProductOutbox alreadySent = saveOutbox("2");
        alreadySent.markSent();

        outboxPollingScheduler.sendPendingEvents();

        @SuppressWarnings("unchecked")
        final org.mockito.ArgumentCaptor<List<ProductOutbox>> captor =
            org.mockito.ArgumentCaptor.forClass(List.class);

        then(recommendationHttpClient).should().sendProductEvents(captor.capture());

        assertThat(captor.getValue())
            .extracting(ProductOutbox::getAggregateId)
            .containsExactly("1");
    }
}
