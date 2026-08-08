package shop.dear.common.event.settlement;

import java.math.BigDecimal;

public record SettlementPayoutItem(
    Long settlementId,
    BigDecimal netAmount
) {
}
