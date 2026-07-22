package shop.dear.commerce.financial.wallet.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.commerce.financial.wallet.domain.costant.WalletLogType;
import shop.dear.common.audit.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "wallet_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "settlement_id")
    private Long settlementId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private WalletLogType type;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    private WalletLog(
            final Wallet wallet,
            final WalletLogType type,
            final BigDecimal amount,
            final Long settlementId,
            final Long paymentId
    ) {
            this.wallet = wallet;
            this.type = type;
            this.amount = amount;
            this.settlementId = settlementId;
            this.paymentId = paymentId;
    }

    public static WalletLog create(
            final Wallet wallet,
            final WalletLogType type,
            final BigDecimal amount,
            final Long settlementId,
            final Long paymentId
    ) {
            return new WalletLog(wallet, type, amount, settlementId, paymentId);
    }
}
