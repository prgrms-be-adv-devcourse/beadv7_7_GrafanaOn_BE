package shop.dear.commerce.order.offer.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.order.offer.application.dto.CreateOfferCommand;
import shop.dear.commerce.order.offer.application.dto.CreateOfferSnapshotCommand;
import shop.dear.commerce.order.offer.application.port.OfferEventPublisher;
import shop.dear.commerce.order.offer.domain.constant.OfferStatus;
import shop.dear.commerce.order.offer.domain.exception.OfferErrorCode;
import shop.dear.commerce.order.offer.domain.model.Offer;
import shop.dear.commerce.order.offer.domain.repository.OfferRepository;
import shop.dear.commerce.order.offersnapshot.domain.exception.OfferSnapshotErrorCode;
import shop.dear.commerce.order.offersnapshot.domain.model.OfferSnapshot;
import shop.dear.commerce.order.offersnapshot.domain.repository.OfferSnapshotRepository;
import shop.dear.commerce.order.offer.application.port.MemberPort;
import shop.dear.commerce.order.offer.application.port.ProductPort;
import shop.dear.commerce.order.offer.application.port.dto.ProductInfo;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.event.order.OrderType;
import shop.dear.common.exception.BusinessException;
import shop.dear.common.exception.CommonErrorCode;

import java.math.BigDecimal;
import java.util.List;

import static shop.dear.commerce.order.offer.domain.exception.OfferErrorCode.NOT_OFFER_SELLER;
import static shop.dear.commerce.order.offer.domain.exception.OfferErrorCode.OFFER_NOT_FOUND;

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
    private final OfferSnapshotRepository offerSnapshotRepository;
    private final ProductPort productPort;
    private final MemberPort memberPort;

    public boolean existsActiveOfferByProductId(final Long productId) {
        return offerRepository.existsByProductIdAndStatusIn(productId, ACTIVE_STATUSES);
    }

    @Transactional
    public OfferSnapshot createOfferSnapshot(final CreateOfferSnapshotCommand command) {
        validateWriterId(command.writerId());
        validateMemberExists(command.writerId());

        final ProductInfo product = productPort.getProduct(command.productId());

        final OfferSnapshot snapshot = offerSnapshotRepository.findFirstByWriterIdAndProductId(
                        command.writerId(), command.productId())
                .map(existing -> {
                    existing.updateSnapshot(product.modelNumber(), product.price());
                    return existing;
                })
                .orElseGet(() -> OfferSnapshot.create(
                        product.sellerId(),
                        command.writerId(),
                        command.productId(),
                        product.modelNumber(),
                        product.price()
                ));

        return offerSnapshotRepository.save(snapshot);
    }

    private void validateWriterId(final Long writerId) {
        if (writerId == null || writerId <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
    }

    private void validateMemberExists(final Long memberId) {
        memberPort.validateMemberExists(memberId);
    }

    @Transactional
    public Offer createOffer(final CreateOfferCommand command) {
        validateWriterId(command.writerId());
        validateMemberExists(command.writerId());

        final OfferSnapshot snapshot = offerSnapshotRepository.findById(command.snapshotId())
                .orElseThrow(() -> new BusinessException(OfferSnapshotErrorCode.OFFER_SNAPSHOT_NOT_FOUND));

        final Offer offer = Offer.create(
                command.writerId(),
                snapshot.getSellerId(),
                snapshot.getProductId(),
                snapshot.getId(),
                snapshot.getPriceSnapshot(),
                command.title(),
                command.story(),
                command.delivery()
        );

        final Offer savedOffer = offerRepository.save(offer);
        snapshot.linkToOffer(savedOffer.getId(), command.writerId());

        final ProductInfo currentProduct = productPort.getProduct(snapshot.getProductId());
        validatePriceSnapshot(snapshot.getPriceSnapshot(), currentProduct.price());

        return savedOffer;
    }

    private void validatePriceSnapshot(final BigDecimal snapshotPrice,
                                       final BigDecimal currentPrice) {
        if (snapshotPrice.compareTo(currentPrice) != 0) {
            throw new BusinessException(OfferErrorCode.OFFER_PRICE_MISMATCH);
        }
    }

    @Transactional
    public void acceptOffer(final Long offerId, final Long memberId) {
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

    @Transactional
    public void rejectOffer(final Long offerId, final Long memberId) {
        final Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new BusinessException(OFFER_NOT_FOUND));

        if (!offer.isSeller(memberId)) {
            throw new BusinessException(NOT_OFFER_SELLER);
        }

        offer.reject();
        offerRepository.save(offer);
    }
}
