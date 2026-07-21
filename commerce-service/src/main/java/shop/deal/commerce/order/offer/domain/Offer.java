package shop.deal.commerce.order.offer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.deal.common.audit.BaseEntity;
import shop.deal.common.exception.BusinessException;
import shop.deal.commerce.order.offer.domain.exception.OfferErrorCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Offer extends BaseEntity {

    private static final String NUMBER_PREFIX = "OF";
    private static final DateTimeFormatter NUMBER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

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

    private Offer(
        final Long buyerId,
        final Long sellerId,
        final Long productId,
        final BigDecimal offerAmount,
        final String offerTitle,
        final String offerStory,
        final String delivery
    ) {
        this.number = generateNumber();
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.offerAmount = offerAmount;
        this.offerTitle = offerTitle;
        this.offerStory = offerStory;
        this.delivery = delivery;
        this.offerStatus = OfferStatus.PENDING;
    }

    public static Offer create(
        final Long buyerId,
        final Long sellerId,
        final Long productId,
        final BigDecimal offerAmount,
        final String offerTitle,
        final String offerStory,
        final String delivery
    ) {
        return new Offer(buyerId, sellerId, productId, offerAmount, offerTitle, offerStory, delivery);
    }

    private static String generateNumber() {
        final String timestamp = LocalDateTime.now().format(NUMBER_DATE_FORMATTER);
        final String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return NUMBER_PREFIX + timestamp + random;
    }

    public void accept() {
        validateOfferStatus(OfferStatus.PENDING);
        this.offerStatus = OfferStatus.ACCEPTED;
    }

    public void reject() {
        validateOfferStatus(OfferStatus.PENDING);
        this.offerStatus = OfferStatus.REJECTED;
    }

    public void cancel() {
        validateOfferStatus(OfferStatus.PENDING);
        this.offerStatus = OfferStatus.CANCELLED;
    }

    public void requestPayment() {
        validateOfferStatus(OfferStatus.ACCEPTED);
        this.paymentStatus = PaymentStatus.PAYMENT_PENDING;
    }

    public void markPaid() {
        validatePaymentStatus(PaymentStatus.PAYMENT_PENDING);
        this.paymentStatus = PaymentStatus.PAID;
    }

    public void markPaymentFailed() {
        validatePaymentStatus(PaymentStatus.PAYMENT_PENDING);
        this.paymentStatus = PaymentStatus.PAYMENT_FAILED;
    }

    private void validateOfferStatus(final OfferStatus expected) {
        if (this.offerStatus != expected) {
            throw new BusinessException(OfferErrorCode.INVALID_OFFER_STATUS_TRANSITION);
        }
    }

    private void validatePaymentStatus(final PaymentStatus expected) {
        if (this.paymentStatus != expected) {
            throw new BusinessException(OfferErrorCode.INVALID_OFFER_PAYMENT_STATUS_TRANSITION);
        }
    }
}
