package shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementInboxJpaRepository extends JpaRepository<SettlementInbox, Long> {

    Optional<SettlementInbox> findByEventId(String eventId);
}
