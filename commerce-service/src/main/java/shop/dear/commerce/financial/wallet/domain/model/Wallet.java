package shop.dear.commerce.financial.wallet.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.commerce.financial.wallet.domain.costant.WalletLogType;
import shop.dear.common.audit.BaseEntity;
import shop.dear.common.exception.BusinessException;
import shop.dear.commerce.financial.wallet.domain.exception.WalletErrorCode;

import java.math.BigDecimal;

@Entity
@Table(name = "wallet")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseEntity {

    private static final int MAX_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Version
    private Long version;

    @Column(name = "balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal balance;

    @Column(name = "held_balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal heldBalance;

    private Wallet(final Long memberId) {
        this.memberId = memberId;
        this.balance = BigDecimal.ZERO;
        this.heldBalance = BigDecimal.ZERO;
    }

    private void validate(final BigDecimal amount, final Long referenceId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || amount.scale() > MAX_SCALE) {
            throw new BusinessException(WalletErrorCode.INVALID_AMOUNT);
        }
        if (referenceId == null) {
            throw new BusinessException(WalletErrorCode.INVALID_REFERENCE_ID);
        }
    }

    public static Wallet create(final Long memberId) {
        return new Wallet(memberId);
    }

    public WalletLog pay(final BigDecimal amount, final Long referenceId) {
        validate(amount, referenceId);
        if (this.balance.compareTo(amount) < 0) {
            throw new BusinessException(WalletErrorCode.INSUFFICIENT_BALANCE);
        }
        this.balance = this.balance.subtract(amount);
        return WalletLog.create(this, WalletLogType.PAYMENT, amount, referenceId);
    }

    public WalletLog settle(final BigDecimal amount, final Long referenceId) {
        validate(amount, referenceId);
        if (this.balance.compareTo(amount) < 0) {
            throw new BusinessException(WalletErrorCode.INSUFFICIENT_BALANCE);
        }
        this.balance = this.balance.subtract(amount);
        return WalletLog.create(this, WalletLogType.SETTLEMENT, amount, referenceId);
    }

    public WalletLog topup(final BigDecimal amount, final Long referenceId) {
        validate(amount, referenceId);
        this.balance = this.balance.add(amount);
        return WalletLog.create(this, WalletLogType.TOPUP, amount, referenceId);
    }

    public WalletLog hold(final BigDecimal amount, final Long referenceId) {
        validate(amount, referenceId);
        if (this.balance.compareTo(amount) < 0) {
            throw new BusinessException(WalletErrorCode.INSUFFICIENT_BALANCE);
        }
        this.balance = this.balance.subtract(amount); // 릴리즈 시 반환
        this.heldBalance = this.heldBalance.add(amount);
        return WalletLog.create(this, WalletLogType.HOLD, amount, referenceId);
    }

    public WalletLog release(final BigDecimal amount, final Long referenceId) {
        validate(amount, referenceId);
        if (this.heldBalance.compareTo(amount) < 0) {
            throw new BusinessException(WalletErrorCode.INSUFFICIENT_HELD_BALANCE);
        }
        this.heldBalance = this.heldBalance.subtract(amount);
        this.balance = this.balance.add(amount);
        return WalletLog.create(this, WalletLogType.RELEASE, amount, referenceId);
    }

    public WalletLog earn(final BigDecimal amount, final Long referenceId) {
        validate(amount, referenceId);
        this.balance = this.balance.add(amount);
        return WalletLog.create(this, WalletLogType.PROCEEDS, amount, referenceId);
    }
}
