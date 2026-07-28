package shop.dear.commerce.financial.payment.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.commerce.financial.payment.domain.constant.PGPaymentStatus;
import shop.dear.common.audit.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "pg_payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PGPayment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 30)
    private PGPaymentStatus state;

    @Column(name = "transaction_key", nullable = false, length = 255)
    private String transaction_key;

    private PGPayment(
            final Payment payment,
            final BigDecimal amount,
            final String transaction_key
    ) {
            this.payment = payment;
            this.amount = amount;
            this.transaction_key = transaction_key;
            this.state = PGPaymentStatus.READY;
    }

    public static PGPayment create(
            final Payment payment,
            final BigDecimal amount,
            final String transaction_key
    ) {
            return new PGPayment(payment, amount, transaction_key);
    }
}
