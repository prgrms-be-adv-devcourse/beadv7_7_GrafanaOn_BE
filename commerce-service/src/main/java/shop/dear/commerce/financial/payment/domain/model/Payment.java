package shop.dear.commerce.financial.payment.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.commerce.financial.payment.domain.constant.PaymentPurpose;
import shop.dear.commerce.financial.payment.domain.constant.PaymentStatus;
import shop.dear.commerce.financial.payment.domain.exception.PaymentErrorCode;
import shop.dear.common.audit.BaseEntity;
import shop.dear.common.event.order.OrderType;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    private static final int MAX_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(
            mappedBy = "payment",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    private PGPayment pgPayment;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private PaymentPurpose purpose;

    @Column(name = "order_id")
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 30)
    private OrderType orderType;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 30)
    private PaymentStatus state;

    private Payment(
            final Long walletId,
            final PaymentPurpose purpose,
            final Long orderId,
            final OrderType orderType,
            final BigDecimal amount
    ) {
        validate(walletId, purpose, orderId, orderType, amount);

        this.walletId = walletId;
        this.purpose = purpose;
        this.orderId = orderId;
        this.orderType = orderType;
        this.amount = amount;
        this.state = PaymentStatus.PENDING;
    }

    private void validate(
            final Long walletId,
            final PaymentPurpose purpose,
            final Long orderId,
            final OrderType orderType,
            final BigDecimal amount
    ) {
        if (walletId == null) {
            throw new BusinessException(PaymentErrorCode.INVALID_WALLET_ID);
        }

        if (purpose == null) {
            throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_PURPOSE);
        }

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.scale() > MAX_SCALE) {
            throw new BusinessException(PaymentErrorCode.INVALID_AMOUNT);
        }

        if (purpose == PaymentPurpose.ORDER
                && (orderId == null || orderType == null)) {
            throw new BusinessException(PaymentErrorCode.INVALID_ORDER_REFERENCE);
        }

        if (purpose == PaymentPurpose.TOPUP
                && (orderId != null || orderType != null)) {
            throw new BusinessException(PaymentErrorCode.INVALID_ORDER_REFERENCE);
        }
    }

    public void preparePgPayment() {
        if (this.purpose != PaymentPurpose.TOPUP) {
            throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_PURPOSE);
        }

        if (this.pgPayment != null) {
            throw new BusinessException(PaymentErrorCode.PG_PAYMENT_ALREADY_PREPARED);
        }

        this.pgPayment = PGPayment.create(this);
    }

    public static Payment createTopUp(
            final Long walletId,
            final BigDecimal amount
    ) {
        return new Payment(
                walletId,
                PaymentPurpose.TOPUP,
                null,
                null,
                amount
        );
    }

    public static Payment createOrderPayment(
            final Long walletId,
            final Long orderId,
            final OrderType orderType,
            final BigDecimal amount
    ) {
        return new Payment(
                walletId,
                PaymentPurpose.ORDER,
                orderId,
                orderType,
                amount
        );
    }
}
