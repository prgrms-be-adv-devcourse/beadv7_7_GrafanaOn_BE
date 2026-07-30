package shop.dear.commerce.financial.wallet.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.commerce.financial.payment.application.event.WalletTopUpRequestedEvent;
import shop.dear.commerce.financial.wallet.application.WalletService;
import shop.dear.commerce.financial.wallet.application.dto.TopUpCommand;
import shop.dear.commerce.financial.wallet.application.event.WalletTopUpSucceededEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletTopUpRequestedEventListener {

    private final WalletService walletService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(final WalletTopUpRequestedEvent event) {
        try {
            walletService.topUp(
                    new TopUpCommand(
                            event.memberId(),
                            event.amount(),
                            event.paymentId()
                    )
            );

            applicationEventPublisher.publishEvent(
                    new WalletTopUpSucceededEvent(event.paymentId())
            );
        } catch (final RuntimeException exception) {
            log.error(
                    "예치금 충전 처리 실패. paymentId={}",
                    event.paymentId(),
                    exception
            );
        }
    }
}
