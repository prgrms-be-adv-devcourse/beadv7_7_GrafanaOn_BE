package shop.dear.common.event.settlement;

import java.math.BigDecimal;
import java.util.List;

// 정산 대기 건에 대한 예치금 지급 요청 (settlement -> wallet)
public record SettlementPayoutEvent(
    Long walletId,
    List<SettlementPayoutItem> items
) {
    public List<Long> settlementIds() {
        return items.stream()
            .map(SettlementPayoutItem::settlementId)
            .toList();
    }

    public BigDecimal totalAmount() {
        return items.stream()
            .map(SettlementPayoutItem::netAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
