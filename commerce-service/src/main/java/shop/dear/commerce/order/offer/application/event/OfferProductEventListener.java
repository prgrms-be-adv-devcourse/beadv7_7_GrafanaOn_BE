package shop.dear.commerce.order.offer.application.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.common.event.product.ProductDeletedEvent;
import shop.dear.commerce.order.offer.domain.constant.OfferStatus;
import shop.dear.commerce.order.offer.domain.model.Offer;
import shop.dear.commerce.order.offer.domain.repository.OfferRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OfferProductEventListener {

    private static final List<OfferStatus> ACTIVE_STATUSES = List.of(
            OfferStatus.PENDING,
            OfferStatus.ACCEPTED
    );

    private final OfferRepository offerRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductDeleted(final ProductDeletedEvent event) {
        final List<Offer> offers = offerRepository.findByProductIdAndStatusInOrderByInsertedAtDesc(
                event.productId(), ACTIVE_STATUSES);

        offers.forEach(Offer::markProductDeleted);
    }
}
