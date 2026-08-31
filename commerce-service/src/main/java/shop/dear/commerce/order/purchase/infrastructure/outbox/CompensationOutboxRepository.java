package shop.dear.commerce.order.purchase.infrastructure.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompensationOutboxRepository extends JpaRepository<CompensationOutbox, Long> {

    // 스케줄러로 가져갈 배치 데이터. status SUCCESS 만 제외하고 조회 (PENDING, FAILED 모두 재시도 대상)
    @Query("select o from CompensationOutbox o where o.status <> :completedStatus order by o.insertedAt asc")
    List<CompensationOutbox> findBatchForRetry(
        @Param("completedStatus") final CompensationOutboxStatus completedStatus,
        final Pageable pageable
    );
}
