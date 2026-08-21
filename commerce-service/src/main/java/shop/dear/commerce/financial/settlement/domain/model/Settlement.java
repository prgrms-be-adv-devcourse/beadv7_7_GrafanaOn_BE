package shop.dear.commerce.financial.settlement.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.commerce.financial.settlement.domain.constant.SettlementStatus;
import shop.dear.audit.BaseEntity;
import shop.dear.commerce.financial.settlement.domain.exception.SettlementErrorCode;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;

@Entity
@Table(name = "settlement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_policy_id", nullable = false)
    private Long settlementPolicyId;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    // purchaseId 와 offerId 중 하나만 필요
    @Column(name = "purchase_id")
    private Long purchaseId;

    @Column(name = "offer_id")
    private Long offerId;

    @Column(name = "gross_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal grossAmount;

    @Column(name = "fee_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal feeAmount;

    @Column(name = "net_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 30)
    private SettlementStatus state;

    private Settlement(
            final Long settlementPolicyId,
            final Long walletId,
            final Long purchaseId,
            final Long offerId,
            final BigDecimal grossAmount,
            final BigDecimal feeAmount,
            final BigDecimal netAmount
    ) {
            this.settlementPolicyId = settlementPolicyId;
            this.walletId = walletId;
            this.purchaseId = purchaseId;
            this.offerId = offerId;
            this.grossAmount = grossAmount;
            this.feeAmount = feeAmount;
            this.netAmount = netAmount;
            this.state = SettlementStatus.CREATED;
    }

    public static Settlement create(
            final Long settlementPolicyId,
            final Long walletId,
            final Long purchaseId,
            final Long offerId,
            final BigDecimal grossAmount,
            final BigDecimal feeAmount,
            final BigDecimal netAmount
    ) {
        return new Settlement(
                settlementPolicyId,
                walletId,
                purchaseId,
                offerId,
                grossAmount,
                feeAmount,
                netAmount
        );
    }

    public void accumulated() {
        validateState(SettlementStatus.CREATED);

        this.state = SettlementStatus.PENDING;
    }

    public void payout() {
        validateState(SettlementStatus.PENDING);

        this.state = SettlementStatus.COMPLETED;
    }

    public void fail() {
        validateState(SettlementStatus.PENDING);

        this.state = SettlementStatus.FAILED;
    }

    private void validateState(final SettlementStatus expected) {
        if (this.state != expected) {
            throw new BusinessException(SettlementErrorCode.INVALID_SETTLEMENT_STATUS_TRANSITION);
        }
    }
}
