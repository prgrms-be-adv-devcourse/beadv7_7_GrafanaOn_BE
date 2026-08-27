package shop.dear.recommendation.infrastructure.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shop.dear.recommendation.application.handler.ProductEventHandler;


import shop.dear.recommendation.application.dto.ProductEvent;
import shop.dear.recommendation.application.port.ProductEventStore;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "recommendation.pipeline", name = "enabled", havingValue = "true")
public class SpringEmbeddingScheduler {

	private final ProductEventStore eventStore;
	private final ProductEventHandler productEventHandler;
	private final int batchSize;

	public SpringEmbeddingScheduler(
		final ProductEventStore eventStore,
		final ProductEventHandler productEventHandler,
		@Value("${recommendation.inbox.batch-size}") final int batchSize
	) {
		this.eventStore = eventStore;
		this.productEventHandler = productEventHandler;
		this.batchSize = batchSize;
	}

	@Scheduled(fixedDelayString = "${recommendation.inbox.poll-interval-ms}")
	public void pollPendingEvents() {
		final List<ProductEvent> pending = this.eventStore.findPending(this.batchSize);

		if (pending.isEmpty()) {
			return;
		}

		log.info("이벤트 {}건 처리 시작", pending.size());

		for (final ProductEvent event : pending) {
			try {
				this.productEventHandler.process(event);
			} catch (final Exception e) {
				log.error("이벤트 {} 처리 중 예기치 못한 예외", event.eventId(), e);
			}
		}
	}
}
