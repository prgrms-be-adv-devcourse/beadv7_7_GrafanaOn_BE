package shop.dear.commerce.product.infrastructure.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductInboxRepository extends JpaRepository<ProductInbox, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO product_inbox (
            consumer_name, event_id, event_type, payload, status, retry_count, inserted_at, updated_at
        )
        VALUES (
            :consumerName, :eventId, :eventType, cast(:payload as jsonb), 'PENDING', 0, now(), now()
        )
        ON CONFLICT (consumer_name, event_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
        @Param("consumerName") final String consumerName,
        @Param("eventId") final String eventId,
        @Param("eventType") final String eventType,
        @Param("payload") final String payload
    );
}
