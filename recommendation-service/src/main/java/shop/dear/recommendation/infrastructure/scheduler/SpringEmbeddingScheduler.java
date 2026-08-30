package shop.dear.recommendation.infrastructure.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shop.dear.recommendation.application.handler.ProductEventHandler;


import shop.dear.recommendation.application.dto.ProductEvent;
import shop.dear.recommendation.application.port.ProductEventStore;

import java.util.List;

@Slf4j
@Component
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

	@Scheduled(
		initialDelayString = "${recommendation.inbox.initial-delay-ms}",
		fixedDelayString = "${recommendation.inbox.poll-interval-ms}"
	)
	public void pollEvents() {
		final List<ProductEvent> events = this.eventStore.findProcessable(this.batchSize);

		if (events.isEmpty()) {
			return;
		}

		log.info("이벤트 {}건 처리 시작", events.size());

		for (final ProductEvent event : events) {
			try {
				this.productEventHandler.process(event);
			} catch (final Exception e) {
				log.error("이벤트 {} 처리 중 예기치 못한 예외", event.eventId(), e);
			}
		}
	}
}
