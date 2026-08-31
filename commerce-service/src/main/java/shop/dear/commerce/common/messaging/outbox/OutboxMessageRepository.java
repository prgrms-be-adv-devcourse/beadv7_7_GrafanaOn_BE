package shop.dear.commerce.common.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {

    List<OutboxMessage> findTop100ByStatusOrderByInsertedAtAsc(OutboxMessageStatus status);
}
