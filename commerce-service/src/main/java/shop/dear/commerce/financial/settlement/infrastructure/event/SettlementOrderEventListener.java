package shop.dear.commerce.financial.settlement.infrastructure.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.commerce.financial.settlement.application.SettlementService;

@Component
@RequiredArgsConstructor
public class SettlementOrderEventListener {

    private final SettlementService settlementService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(final FinishedOrderEvent event) {
        settlementService.createPendingSettlement(event);
    }
}
