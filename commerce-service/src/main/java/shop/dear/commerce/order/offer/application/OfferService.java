package shop.dear.commerce.order.offer.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import shop.dear.commerce.order.offer.domain.constant.OfferReleaseReason;
import shop.dear.common.event.financial.PaymentHoldRequestedEvent;
import shop.dear.common.event.financial.PaymentReleaseRequestedEvent;
import shop.dear.common.event.financial.PaymentRequestedEvent;
import shop.dear.common.type.OrderType;
import shop.dear.common.exception.BusinessException;
import shop.dear.common.exception.CommonErrorCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

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
        validateMemberExists();

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

    private void validateMemberExists() {
        memberPort.validateMemberExists();
    }

    @Transactional
    public Offer createOffer(final CreateOfferCommand command) {
        validateWriterId(command.writerId());
        validateMemberExists();

        final OfferSnapshot snapshot = offerSnapshotRepository.findById(command.snapshotId())
                .orElseThrow(() -> new BusinessException(OfferSnapshotErrorCode.OFFER_SNAPSHOT_NOT_FOUND));

        final ProductInfo currentProduct = productPort.getProduct(snapshot.getProductId());
        validatePriceSnapshot(snapshot.getPriceSnapshot(), currentProduct.price());

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

        offerEventPublisher.publish(new PaymentHoldRequestedEvent(
                savedOffer.getId(),
                savedOffer.getBuyerId(),
                savedOffer.getAmount()
        ));

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

        offerEventPublisher.publish(new PaymentRequestedEvent(
                offer.getId(),
                OrderType.OFFER.name(),
                offer.getBuyerId(),
                offer.getAmount()
        ));

        releaseOtherPendingOffers(offer);
    }

    private void releaseOtherPendingOffers(final Offer acceptedOffer) {
        final List<Offer> otherOffers = offerRepository.findByProductIdAndStatusInOrderByInsertedAtDesc(
                acceptedOffer.getProductId(), List.of(OfferStatus.PENDING));

        for (final Offer otherOffer : otherOffers) {
            if (Objects.equals(otherOffer.getId(), acceptedOffer.getId())) {
                continue;
            }

            otherOffer.reject();
            offerRepository.save(otherOffer);

            offerEventPublisher.publish(new PaymentReleaseRequestedEvent(
                    otherOffer.getId(),
                    otherOffer.getBuyerId(),
                    otherOffer.getAmount(),
                    OfferReleaseReason.OFFER_OUTBID.name()
            ));
        }
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

        offerEventPublisher.publish(new PaymentReleaseRequestedEvent(
                offer.getId(),
                offer.getBuyerId(),
                offer.getAmount(),
                OfferReleaseReason.OFFER_REJECTED.name()
        ));
    }

    public Offer findOfferById(final Long offerId, final Long memberId) {
        final Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new BusinessException(OFFER_NOT_FOUND));

        if (!offer.isSeller(memberId) && !offer.getBuyerId().equals(memberId)) {
            throw new BusinessException(NOT_OFFER_SELLER);
        }

        return offer;
    }

    public Page<Offer> findOffersByProductId(
            final Long memberId,
            final Long productId,
            final List<OfferStatus> statuses,
            final Pageable pageable
    ) {
        final ProductInfo product = productPort.getProduct(productId);
        if (!product.sellerId().equals(memberId)) {
            throw new BusinessException(NOT_OFFER_SELLER);
        }

        if (statuses == null || statuses.isEmpty()) {
            return offerRepository.findByProductIdOrderByInsertedAtDesc(productId, pageable);
        }
        return offerRepository.findByProductIdAndStatusInOrderByInsertedAtDesc(productId, statuses, pageable);
    }
}
