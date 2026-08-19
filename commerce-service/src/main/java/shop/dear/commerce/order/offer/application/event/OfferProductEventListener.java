package shop.dear.commerce.order.offer.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.common.event.product.ProductDeletedEvent;
import shop.dear.commerce.order.offer.application.OfferProductDeletionProcessor;
import shop.dear.commerce.order.offer.domain.constant.OfferStatus;
import shop.dear.commerce.order.offer.domain.model.Offer;
import shop.dear.commerce.order.offer.domain.repository.OfferRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfferProductEventListener {

    private static final List<OfferStatus> ACTIVE_STATUSES = List.of(
            OfferStatus.PENDING,
            OfferStatus.ACCEPTED
    );

    private final OfferRepository offerRepository;
    private final OfferProductDeletionProcessor offerProductDeletionProcessor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductDeleted(final ProductDeletedEvent event) {
        final List<Offer> offers = offerRepository.findByProductIdAndStatusInOrderByInsertedAtDesc(
                event.productId(), ACTIVE_STATUSES);

        for (final Offer offer : offers) {
            try {
                offerProductDeletionProcessor.markProductDeleted(offer.getId());
            } catch (final Exception e) {
                log.error("상품 삭제로 인한 오퍼 처리 중 예외가 발생하여 건너뜁니다. offerId={}", offer.getId(), e);
            }
        }
    }
}
