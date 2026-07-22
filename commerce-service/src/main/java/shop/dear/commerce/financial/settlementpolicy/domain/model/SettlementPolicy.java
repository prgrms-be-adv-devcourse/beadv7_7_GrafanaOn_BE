package shop.dear.commerce.financial.settlementpolicy.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.common.audit.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "settlement_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fee_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal feeRate;

    @Column(name = "fixed_fee", precision = 15, scale = 2, nullable = false)
    private BigDecimal fixedFee;

    private SettlementPolicy(final BigDecimal feeRate, final BigDecimal fixedFee) {
        this.feeRate = feeRate;
        this.fixedFee = fixedFee;
    }

    public static SettlementPolicy create(final BigDecimal feeRate, final BigDecimal fixedFee) {
        return new SettlementPolicy(feeRate, fixedFee);
    }
}
