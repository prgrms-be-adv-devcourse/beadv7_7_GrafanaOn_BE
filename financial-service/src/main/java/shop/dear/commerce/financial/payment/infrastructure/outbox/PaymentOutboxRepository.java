package shop.dear.commerce.financial.payment.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentOutboxRepository
        extends JpaRepository<PaymentOutbox, Long> {

    List<PaymentOutbox> findTop100ByStatusInOrderByInsertedAtAsc(
            List<PaymentOutboxStatus> statuses
    );
}
