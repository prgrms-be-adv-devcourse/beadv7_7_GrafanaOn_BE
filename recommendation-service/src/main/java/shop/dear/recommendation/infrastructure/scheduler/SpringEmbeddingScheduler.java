package shop.dear.recommendation.infrastructure.scheduler;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import shop.dear.recommendation.application.scheduller.EmbeddingScheduller;
import shop.dear.recommendation.domain.constant.InboxStatus;
import shop.dear.recommendation.domain.model.RecommendationInbox;
import shop.dear.recommendation.domain.repository.RecommendationInboxRepository;

@Slf4j
@Component
@ConditionalOnProperty(name = "recommendation.pipeline.enabled", havingValue = "true")
public class SpringEmbeddingScheduler {

	private final RecommendationInboxRepository inboxRepository;
	private final EmbeddingScheduller eventProcessor;
	private final int batchSize;

	public SpringEmbeddingScheduler(
		RecommendationInboxRepository inboxRepository,
		EmbeddingScheduller eventProcessor,
		@Value("${recommendation.inbox.batch-size}") int batchSize
	) {
		this.inboxRepository = inboxRepository;
		this.eventProcessor = eventProcessor;
		this.batchSize = batchSize;
	}

	@Scheduled(fixedDelayString = "${recommendation.inbox.poll-interval-ms}")
	public void processPendingEvents() {
		List<RecommendationInbox> events = this.inboxRepository.findByStatusOrderByOccurredAtAsc(InboxStatus.PENDING, this.batchSize);

		if (events.isEmpty()) {
			return;
		}

		log.info("Inbox PENDING {}건 처리 시작", events.size());

		for (RecommendationInbox event : events) {
			try {
				this.eventProcessor.process(event);
			} catch (Exception e) {
				// 1건이 터져도 나머지는 계속 처리한다.
				log.error("Inbox {} 처리 중 예외", event.getId(), e);
			}
		}
	}
}
