package shop.dear.commerce.financial.wallet.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import shop.dear.commerce.financial.wallet.application.WalletService;
import shop.dear.commerce.financial.wallet.application.dto.EarnCommand;
import shop.dear.common.event.settlement.SettlementPayoutEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletSettlementEventHandler {

    private final WalletService walletService;

    // 월별 정산 집계 금액을 해당 지갑에 예치금으로 충전한다.
    // reference 는 집계 식별자이므로 원장에도 월 1건으로 남는다.
    @EventListener
    public void handle(final SettlementPayoutEvent event) {

        walletService.earn(new EarnCommand(
            event.walletId(),
            event.netAmount(),
            event.settlementBatchId()
        ));

        log.info("[Wallet] 정산 지급 충전 - walletId={}, period={}, batchId={}, amount={}",
            event.walletId(), event.period(), event.settlementBatchId(), event.netAmount());
    }
}
