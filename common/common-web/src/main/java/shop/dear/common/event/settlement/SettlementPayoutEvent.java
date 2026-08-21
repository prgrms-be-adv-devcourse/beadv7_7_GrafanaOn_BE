package shop.dear.common.event.settlement;

import java.math.BigDecimal;

// 월별 정산 집계 1건에 대한 예치금 지급 요청 (settlement -> wallet)
// 지급 단위가 집계이므로 지갑 원장에도 집계 1건으로 기록된다.
public record SettlementPayoutEvent(
    Long walletId,
    Long settlementBatchId,
    String period,
    BigDecimal netAmount
) {
}
