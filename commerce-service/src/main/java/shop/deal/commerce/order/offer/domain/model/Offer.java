package shop.deal.commerce.order.offer.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.deal.commerce.order.offer.domain.constant.OfferStatus;
import shop.deal.commerce.order.offer.domain.constant.PaymentStatus;
import shop.deal.common.audit.BaseEntity;
import shop.deal.common.exception.BusinessException;
import shop.deal.commerce.order.offer.domain.exception.OfferErrorCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "offer")
@Entity
public class Offer extends BaseEntity {

    private static final String NUMBER_PREFIX = "OF";
    private static final DateTimeFormatter NUMBER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String number;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String story;

    @Column(nullable = false, length = 255)
    private String delivery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OfferStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus;

    private Offer(
        final Long buyerId,
        final Long sellerId,
        final Long productId,
        final BigDecimal amount,
        final String title,
        final String story,
        final String delivery
    ) {
        this.number = generateNumber();
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.amount = amount;
        this.title = title;
        this.story = story;
        this.delivery = delivery;
        this.status = OfferStatus.PENDING;
        this.paymentStatus = PaymentStatus.PAYMENT_PENDING;
    }

    public static Offer create(
        final Long buyerId,
        final Long sellerId,
        final Long productId,
        final BigDecimal amount,
        final String title,
        final String story,
        final String delivery
    ) {
        return new Offer(buyerId, sellerId, productId, amount, title, story, delivery);
    }

    private static String generateNumber() {
        final String timestamp = LocalDateTime.now().format(NUMBER_DATE_FORMATTER);
        final String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return NUMBER_PREFIX + timestamp + random;
    }

    public void accept() {
        validateOfferStatus(OfferStatus.PENDING);
        validatePaymentStatus(PaymentStatus.PAID);
        this.status = OfferStatus.ACCEPTED;
    }

    private void validateOfferStatus(final OfferStatus expected) {
        if (this.status != expected) {
            throw new BusinessException(OfferErrorCode.INVALID_OFFER_STATUS_TRANSITION);
        }
    }

    private void validatePaymentStatus(final PaymentStatus expected) {
        if (this.paymentStatus != expected) {
            throw new BusinessException(OfferErrorCode.INVALID_OFFER_PAYMENT_STATUS_TRANSITION);
        }
    }

    public void reject() {
        validateOfferStatus(OfferStatus.PENDING);
        this.status = OfferStatus.REJECTED;
    }

    public void cancel() {
        validateOfferStatus(OfferStatus.PENDING);
        this.status = OfferStatus.CANCELLED;
    }

    public void markPaid() {
        validatePaymentStatus(PaymentStatus.PAYMENT_PENDING);
        this.paymentStatus = PaymentStatus.PAID;
    }

    public void markPaymentFailed() {
        validatePaymentStatus(PaymentStatus.PAYMENT_PENDING);
        this.paymentStatus = PaymentStatus.PAYMENT_FAILED;
    }
}
