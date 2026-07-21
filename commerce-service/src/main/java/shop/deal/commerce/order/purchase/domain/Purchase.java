package shop.deal.commerce.order.purchase.domain;

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
import shop.deal.commerce.order.purchase.domain.exception.PurchaseErrorCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Purchase extends BaseEntity {

    private static final DateTimeFormatter NUMBER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PurchaseStatus status;

    @Column(name = "ordered_at", nullable = false)
    private OffsetDateTime orderedAt;

    @Column(name = "payment_due_at")
    private OffsetDateTime paymentDueAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(nullable = false, length = 255)
    private String delivery;

    private Purchase(
        final Long buyerId,
        final Long sellerId,
        final Long productId,
        final BigDecimal amount,
        final String delivery,
        final OffsetDateTime paymentDueAt
    ) {
        this.number = generateNumber(sellerId);
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.amount = amount;
        this.delivery = delivery;
        this.paymentDueAt = paymentDueAt;
        this.status = PurchaseStatus.PENDING_PAYMENT;
        this.orderedAt = OffsetDateTime.now();
    }

    public static Purchase create(
        final Long buyerId,
        final Long sellerId,
        final Long productId,
        final BigDecimal amount,
        final String delivery,
        final OffsetDateTime paymentDueAt
    ) {
        return new Purchase(buyerId, sellerId, productId, amount, delivery, paymentDueAt);
    }

    private static String generateNumber(final Long sellerId) {
        final String date = LocalDate.now().format(NUMBER_DATE_FORMATTER);
        final String sellerCode = Long.toString(sellerId, 36).toUpperCase();
        final String random = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        return date + "-" + sellerCode + random;
    }

    public void pay() {
        validateStatus(PurchaseStatus.PENDING_PAYMENT);
        this.status = PurchaseStatus.PAID;
        this.paidAt = OffsetDateTime.now();
    }

    public void failPayment() {
        validateStatus(PurchaseStatus.PENDING_PAYMENT);
        this.status = PurchaseStatus.PAYMENT_FAILED;
    }

    public void expire() {
        validateStatus(PurchaseStatus.PENDING_PAYMENT);
        this.status = PurchaseStatus.EXPIRED;
    }

    public void cancel() {
        validateStatus(PurchaseErrorCode.PURCHASE_CANNOT_BE_CANCELLED, PurchaseStatus.PENDING_PAYMENT, PurchaseStatus.PAID);
        this.status = PurchaseStatus.CANCELLED;
    }

    public void confirmPurchase() {
        validateStatus(PurchaseStatus.PAID);
        this.status = PurchaseStatus.PURCHASE_CONFIRMED;
    }

    public void refund() {
        validateStatus(PurchaseErrorCode.PURCHASE_CANNOT_BE_REFUNDED, PurchaseStatus.PAID, PurchaseStatus.PURCHASE_CONFIRMED);
        this.status = PurchaseStatus.REFUNDED;
    }

    private void validateStatus(final PurchaseStatus... expected) {
        validateStatus(PurchaseErrorCode.INVALID_PURCHASE_STATUS_TRANSITION, expected);
    }

    private void validateStatus(final PurchaseErrorCode errorCode, final PurchaseStatus... expected) {
        for (final PurchaseStatus status : expected) {
            if (this.status == status) {
                return;
            }
        }
        throw new BusinessException(errorCode);
    }
}
