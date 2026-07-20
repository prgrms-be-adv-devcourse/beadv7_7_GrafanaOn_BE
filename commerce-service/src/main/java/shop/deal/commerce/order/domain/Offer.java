package shop.deal.commerce.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.deal.common.exception.BusinessException;
import shop.deal.commerce.order.domain.exception.OrderErrorCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "offers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String number;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "offer_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal offerAmount;

    @Column(name = "offer_title", nullable = false, columnDefinition = "TEXT")
    private String offerTitle;

    @Column(name = "offer_story", nullable = false, columnDefinition = "TEXT")
    private String offerStory;

    @Column(nullable = false, length = 255)
    private String delivery;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_status", nullable = false, length = 30)
    private OfferStatus offerStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 30)
    private PaymentStatus paymentStatus;

    @Column(name = "inserted_at", nullable = false)
    private OffsetDateTime insertedAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    private Offer(
        final String number,
        final Long buyerId,
        final Long sellerId,
        final Long productId,
        final BigDecimal offerAmount,
        final String offerTitle,
        final String offerStory,
        final String delivery
    ) {
        this.number = number;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.offerAmount = offerAmount;
        this.offerTitle = offerTitle;
        this.offerStory = offerStory;
        this.delivery = delivery;
        this.offerStatus = OfferStatus.PENDING;
        this.insertedAt = OffsetDateTime.now();
    }

    public static Offer create(
        final String number,
        final Long buyerId,
        final Long sellerId,
        final Long productId,
        final BigDecimal offerAmount,
        final String offerTitle,
        final String offerStory,
        final String delivery
    ) {
        return new Offer(number, buyerId, sellerId, productId, offerAmount, offerTitle, offerStory, delivery);
    }

    public void accept() {
        validateOfferStatus(OfferStatus.PENDING);
        this.offerStatus = OfferStatus.ACCEPTED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void reject() {
        validateOfferStatus(OfferStatus.PENDING);
        this.offerStatus = OfferStatus.REJECTED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void cancel() {
        validateOfferStatus(OfferStatus.PENDING);
        this.offerStatus = OfferStatus.CANCELLED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markPaymentPending() {
        validateOfferStatus(OfferStatus.ACCEPTED);
        this.paymentStatus = PaymentStatus.PAYMENT_PENDING;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markPaid() {
        validatePaymentStatus(PaymentStatus.PAYMENT_PENDING);
        this.paymentStatus = PaymentStatus.PAID;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markPaymentFailed() {
        validatePaymentStatus(PaymentStatus.PAYMENT_PENDING);
        this.paymentStatus = PaymentStatus.PAYMENT_FAILED;
        this.updatedAt = OffsetDateTime.now();
    }

    private void validateOfferStatus(final OfferStatus expected) {
        if (this.offerStatus != expected) {
            throw new BusinessException(OrderErrorCode.INVALID_OFFER_STATUS_TRANSITION);
        }
    }

    private void validatePaymentStatus(final PaymentStatus expected) {
        if (this.paymentStatus != expected) {
            throw new BusinessException(OrderErrorCode.INVALID_OFFER_PAYMENT_STATUS_TRANSITION);
        }
    }
}
