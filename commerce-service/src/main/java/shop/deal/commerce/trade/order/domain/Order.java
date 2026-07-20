package shop.deal.commerce.trade.order.domain;

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
import shop.deal.common.audit.BaseEntity;
import shop.deal.common.exception.BusinessException;
import shop.deal.commerce.trade.order.domain.exception.OrderErrorCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

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
    private OrderStatus status;

    @Column(name = "ordered_at", nullable = false)
    private OffsetDateTime orderedAt;

    @Column(name = "payment_due_at")
    private OffsetDateTime paymentDueAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(nullable = false, length = 255)
    private String delivery;

    private Order(
        final String number,
        final Long buyerId,
        final Long sellerId,
        final Long productId,
        final BigDecimal amount,
        final String delivery,
        final OffsetDateTime paymentDueAt
    ) {
        this.number = number;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.amount = amount;
        this.delivery = delivery;
        this.paymentDueAt = paymentDueAt;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.orderedAt = OffsetDateTime.now();
    }

    public static Order create(
        final String number,
        final Long buyerId,
        final Long sellerId,
        final Long productId,
        final BigDecimal amount,
        final String delivery,
        final OffsetDateTime paymentDueAt
    ) {
        return new Order(number, buyerId, sellerId, productId, amount, delivery, paymentDueAt);
    }

    public void pay() {
        validateStatus(OrderStatus.PENDING_PAYMENT);
        this.status = OrderStatus.PAID;
        this.paidAt = OffsetDateTime.now();
    }

    public void failPayment() {
        validateStatus(OrderStatus.PENDING_PAYMENT);
        this.status = OrderStatus.PAYMENT_FAILED;
    }

    public void expire() {
        validateStatus(OrderStatus.PENDING_PAYMENT);
        this.status = OrderStatus.EXPIRED;
    }

    public void cancel() {
        validateStatus(OrderErrorCode.ORDER_CANNOT_BE_CANCELLED, OrderStatus.PENDING_PAYMENT, OrderStatus.PAID);
        this.status = OrderStatus.CANCELLED;
    }

    public void confirmPurchase() {
        validateStatus(OrderStatus.PAID);
        this.status = OrderStatus.PURCHASE_CONFIRMED;
    }

    public void refund() {
        validateStatus(OrderErrorCode.ORDER_CANNOT_BE_REFUNDED, OrderStatus.PAID, OrderStatus.PURCHASE_CONFIRMED);
        this.status = OrderStatus.REFUNDED;
    }

    private void validateStatus(final OrderStatus... expected) {
        validateStatus(OrderErrorCode.INVALID_ORDER_STATUS_TRANSITION, expected);
    }

    private void validateStatus(final OrderErrorCode errorCode, final OrderStatus... expected) {
        for (final OrderStatus status : expected) {
            if (this.status == status) {
                return;
            }
        }
        throw new BusinessException(errorCode);
    }
}
