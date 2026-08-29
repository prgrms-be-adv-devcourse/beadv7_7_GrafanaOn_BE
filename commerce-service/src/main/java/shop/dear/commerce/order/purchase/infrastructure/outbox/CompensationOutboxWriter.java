package shop.dear.commerce.order.purchase.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.order.purchase.application.port.dto.ProductStatus;

// createPurchase 롤백과 완전히 별개의 트랜잭션(REQUIRES_NEW)으로 커밋되어야 하는 보상 기록.
// 반드시 별도 Bean으로 분리(자기 자신 호출 시 프록시를 거치지 않아 REQUIRES_NEW가 무시됨)
@Slf4j
@Component
@RequiredArgsConstructor
public class CompensationOutboxWriter {

    private final CompensationOutboxRepository compensationOutboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTradeRevertFailure(final Long productId) {
        compensationOutboxRepository.save(
                CompensationOutbox.of(productId, ProductStatus.ON_SALE.name())
        );

        log.warn("[CompensationOutbox] 구매 생성 실패로 상품 상태 보상 기록 적재. productId={}", productId);
    }
}
