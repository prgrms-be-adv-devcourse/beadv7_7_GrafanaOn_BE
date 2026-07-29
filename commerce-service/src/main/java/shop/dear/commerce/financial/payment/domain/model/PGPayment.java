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

    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    private PGPayment(final Payment payment) {
            this.payment = payment;
            this.state = PGPaymentStatus.READY;
    }

    static PGPayment create(final Payment payment) {
            return new PGPayment(payment);
    }
}
