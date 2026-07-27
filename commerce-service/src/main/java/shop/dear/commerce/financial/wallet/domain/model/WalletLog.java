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

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private WalletLogType type;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    private WalletLog(
            final Wallet wallet,
            final WalletLogType type,
            final BigDecimal amount,
            final Long referenceId
    ) {
            this.wallet = wallet;
            this.type = type;
            this.amount = amount;
            this.referenceId = referenceId;
    }

    static WalletLog create(
            final Wallet wallet,
            final WalletLogType type,
            final BigDecimal amount,
            final Long referenceId
    ) {
            return new WalletLog(wallet, type, amount, referenceId);
    }
}
