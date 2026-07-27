package shop.dear.commerce.order.offer.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.order.common.application.port.MemberPort;
import shop.dear.commerce.order.offer.application.port.OfferEventPublisher;
import shop.dear.commerce.order.offer.domain.constant.OfferStatus;
import shop.dear.commerce.order.offer.domain.model.Offer;
import shop.dear.commerce.order.offer.domain.repository.OfferRepository;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.event.order.OrderType;
import shop.dear.common.exception.BusinessException;

import java.util.List;

import static shop.dear.commerce.order.offer.domain.exception.OfferErrorCode.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OfferService {

    private static final List<OfferStatus> ACTIVE_STATUSES = List.of(
            OfferStatus.PENDING,
            OfferStatus.ACCEPTED
    );

    private final OfferRepository offerRepository;
    private final OfferEventPublisher offerEventPublisher;
    private final MemberPort memberPort;

    public boolean existsActiveOfferByProductId(final Long productId) {
        return offerRepository.existsByProductIdAndStatusIn(productId, ACTIVE_STATUSES);
    }

    @Transactional
    public void acceptOffer(final Long offerId, final Long memberId) {
        validateMemberExists(memberId);

        final Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new BusinessException(OFFER_NOT_FOUND));

        if (!offer.isSeller(memberId)) {
            throw new BusinessException(NOT_OFFER_SELLER);
        }

        offer.accept();

        offerEventPublisher.publish(new FinishedOrderEvent(
                offer.getId(),
                offer.getBuyerId(),
                offer.getSellerId(),
                offer.getProductId(),
                offer.getAmount(),
                OrderType.OFFER
        ));
    }

    private void validateMemberExists(final Long memberId) {
        memberPort.validateMemberExists(memberId);
    }
}
