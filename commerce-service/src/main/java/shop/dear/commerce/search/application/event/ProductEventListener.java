package shop.dear.commerce.search.application.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.common.event.product.ProductChangedEvent;
import shop.dear.common.event.product.ProductDeletedEvent;
import shop.dear.commerce.search.domain.SearchProduct;
import shop.dear.commerce.search.domain.SearchRepository;

@Component
@RequiredArgsConstructor
public class ProductEventListener {

    private static final String SOLD_OUT_STATUS = "SOLD_OUT";

    private final SearchRepository searchRepository;

    // Product 저장 트랜잭션 속 새로운 트랜잭선 발생
    // Product 저장 Tx 시작 -> Product 저장 -> 이벤트 발행 -> Product Tx 커밋
    // -> ProductEventListener 실행 -> 새로운 Tx: search_product 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductChanged(final ProductChangedEvent event) {
        if (SOLD_OUT_STATUS.equals(event.status())) {
            searchRepository.deleteByProductId(event.id());
            return;
        }

        SearchProduct searchProduct = new SearchProduct(
                event.id(),
                event.name(),
                event.modelNumber(),
                event.category(),
                event.releaseDate(),
                event.price(),
                event.saleType(),
                event.viewCount(),
                event.description(),
                event.fullStory(),
                event.productInsertedAt()
        );

        searchRepository.save(searchProduct);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductDeleted(final ProductDeletedEvent event) {
        searchRepository.deleteByProductId(event.productId());
    }
}
