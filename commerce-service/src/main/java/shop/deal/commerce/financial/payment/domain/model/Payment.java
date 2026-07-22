package shop.deal.commerce.financial.payment.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.deal.commerce.financial.payment.domain.constant.PaymentStatus;
import shop.deal.common.audit.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "payment", cascade = CascadeType.PERSIST)
    private PGPayment pgPayment;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    // 구매 상황 아니더라도 충전 가능
    @Column(name = "purchase_id")
    private Long purchaseId;

    @Column(name = "offer_id")
    private Long offerId;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 30)
    private PaymentStatus state;

    private Payment(
            final Long walletId,
            final Long purchaseId,
            final Long offerId,
            final BigDecimal amount
    ) {
            this.walletId = walletId;
            this.purchaseId = purchaseId;
            this.offerId = offerId;
            this.amount = amount;
            this.state = PaymentStatus.PENDING;
    }

    public static Payment create(
            final Long walletId,
            final Long purchased,
            final Long offerId,
            final BigDecimal amount
    ) {
        return new Payment(walletId, purchased, offerId, amount);
    }
}
