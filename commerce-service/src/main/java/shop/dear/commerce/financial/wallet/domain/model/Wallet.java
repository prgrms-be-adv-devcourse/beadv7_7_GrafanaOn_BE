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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "wallet")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseEntity {

    private static final int MAX_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "wallet",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    private List<WalletLog> walletLogs = new ArrayList<>();

    @Column(name = "member_id", unique = true, nullable = false)
    private Long memberId;

    @Version
    private Long version;

    @Column(name = "balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal availableBalance;

    @Column(name = "held_balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal heldBalance;

    private Wallet(final Long memberId) {
        this.memberId = memberId;
        this.availableBalance = BigDecimal.ZERO;
        this.heldBalance = BigDecimal.ZERO;
    }

    private void record(
            final WalletLogType type,
            final BigDecimal amount,
            final Long referenceId
    ) {
        this.walletLogs.add(WalletLog.create(this, type, amount, referenceId));
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

    public List<WalletLog> getWalletLogs() {
        return Collections.unmodifiableList(this.walletLogs);
    }

    public void payAvailable(final BigDecimal amount, final Long referenceId) {
        validate(amount, referenceId);

        if (this.availableBalance.compareTo(amount) < 0) {
            throw new BusinessException(WalletErrorCode.INSUFFICIENT_BALANCE);
        }

        this.availableBalance = this.availableBalance.subtract(amount);
        record(WalletLogType.PAYMENT, amount, referenceId);
    }

    public void payHeld(final BigDecimal amount, final Long referenceId) {
        validate(amount, referenceId);

        if (this.heldBalance.compareTo(amount) < 0) {
            throw new BusinessException(WalletErrorCode.INSUFFICIENT_HELD_BALANCE);
        }

        this.heldBalance = this.heldBalance.subtract(amount);
        record(WalletLogType.PAYMENT, amount, referenceId);
    }

    public void settle(final BigDecimal amount, final Long referenceId) {
        validate(amount, referenceId);

        if (this.availableBalance.compareTo(amount) < 0) {
            throw new BusinessException(WalletErrorCode.INSUFFICIENT_BALANCE);
        }

        this.availableBalance = this.availableBalance.subtract(amount);
        record(WalletLogType.SETTLEMENT, amount, referenceId);
    }

    public void topup(final BigDecimal amount, final Long referenceId) {
        validate(amount, referenceId);

        this.availableBalance = this.availableBalance.add(amount);
        record(WalletLogType.TOPUP, amount, referenceId);
    }

    public void hold(final BigDecimal amount, final Long referenceId) {
        validate(amount, referenceId);

        if (this.availableBalance.compareTo(amount) < 0) {
            throw new BusinessException(WalletErrorCode.INSUFFICIENT_BALANCE);
        }

        this.availableBalance = this.availableBalance.subtract(amount); // 릴리즈 시 반환
        this.heldBalance = this.heldBalance.add(amount);
        record(WalletLogType.HOLD, amount, referenceId);
    }

    public void release(final BigDecimal amount, final Long referenceId) {
        validate(amount, referenceId);

        if (this.heldBalance.compareTo(amount) < 0) {
            throw new BusinessException(WalletErrorCode.INSUFFICIENT_HELD_BALANCE);
        }

        this.heldBalance = this.heldBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
        record(WalletLogType.RELEASE, amount, referenceId);
    }

    public void earn(final BigDecimal amount, final Long referenceId) {
        validate(amount, referenceId);

        this.availableBalance = this.availableBalance.add(amount);
        record(WalletLogType.PROCEEDS, amount, referenceId);
    }
}