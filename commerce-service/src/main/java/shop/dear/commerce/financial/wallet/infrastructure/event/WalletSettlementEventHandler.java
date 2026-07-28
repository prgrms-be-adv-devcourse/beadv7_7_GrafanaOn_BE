package shop.dear.commerce.financial.wallet.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import shop.dear.commerce.financial.wallet.application.WalletService;
import shop.dear.commerce.financial.wallet.application.dto.EarnCommand;
import shop.dear.common.event.settlement.SettlementPayoutEvent;
import shop.dear.common.event.settlement.SettlementPayoutItem;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletSettlementEventHandler {

    private final WalletService walletService;

    // 정산 데이터를 받아 해당 지갑에 예치금을 충전한다.
    @EventListener
    public void handle(final SettlementPayoutEvent event) {

        final List<EarnCommand> commands = event.items().stream()
                .map(item -> toEarnCommand(event.walletId(), item))
                .toList();

        walletService.earnAll(event.walletId(), commands);
    }

    private EarnCommand toEarnCommand(final Long walletId, final SettlementPayoutItem item) {
        return new EarnCommand(walletId, item.netAmount(), item.settlementId());
    }
}
