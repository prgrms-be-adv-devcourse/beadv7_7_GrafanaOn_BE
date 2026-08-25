package shop.dear.commerce.product.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductOutboxRepository extends JpaRepository<ProductOutbox, Long> {

    //스케줄러로 가져갈 배치 데이터. status SENT 만 제외하고 조회
    @Query("select o from ProductOutbox o where o.status <> :completedStatus order by o.insertedAt asc")
    List<ProductOutbox> findBatchForPublish(
        @Param("completedStatus") final ProductOutboxStatus completedStatus,
        final Pageable pageable
    );
}
