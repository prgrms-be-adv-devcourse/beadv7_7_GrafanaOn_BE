package shop.dear.recommendation.application.port;

import shop.dear.recommendation.application.dto.ProductEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductEventStore {

	List<ProductEvent> findPending(int limit);

	Optional<LocalDateTime> findLatestProcessedOccurredAt(String aggregateId);

	void markProcessed(Long eventId);

	void markFailed(Long eventId, String reason);
}
