package shop.dear.commerce.financial.payment.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.commerce.financial.payment.domain.constant.PGPaymentStatus;
import shop.dear.commerce.financial.payment.domain.exception.PaymentErrorCode;
import shop.dear.audit.BaseEntity;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;

@Entity
@Table(name = "pg_payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PGPayment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(
            name = "payment_id",
            nullable = false,
            unique = true
    )
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 30)
    private PGPaymentStatus state;

    @Column(name = "transaction_key", length = 255)
    private String transactionKey;

    @Column(name = "payment_key", unique = true, length = 255)
    private String paymentKey;

    @Column(name = "merchant_order_id", nullable = false, unique = true, length = 64)
    private String merchantOrderId;

    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    private PGPayment(final Payment payment, final String merchantOrderId) {
            this.payment = payment;
            this.merchantOrderId = merchantOrderId;
            this.state = PGPaymentStatus.READY;
    }

    static PGPayment create(final Payment payment, final String merchantOrderId) {
            return new PGPayment(payment, merchantOrderId);
    }

    public void approve(
            final String paymentKey,
            final String transactionKey,
            final BigDecimal approvedAmount
    ) {
        if (this.state != PGPaymentStatus.READY) {
            throw new BusinessException(
                    PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION
            );
        }

        if (approvedAmount == null
                || approvedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(PaymentErrorCode.INVALID_AMOUNT);
        }

        this.paymentKey = paymentKey;
        this.transactionKey = transactionKey;
        this.approvedAmount = approvedAmount;
        this.state = PGPaymentStatus.DONE;
    }
}
