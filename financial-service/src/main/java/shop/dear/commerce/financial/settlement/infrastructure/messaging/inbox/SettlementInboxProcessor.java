package shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.financial.settlement.application.SettlementService;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.SettlementInbox;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.SettlementInboxJpaRepository;
import shop.dear.common.client.InternalCallContext;
import shop.dear.common.event.order.FinishedOrderEvent;
import tools.jackson.databind.ObjectMapper;

// 적재된 inbox 한 건을 정산 건으로 생성
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementInboxProcessor {

    private final SettlementInboxJpaRepository settlementInboxJpaRepository;
    private final SettlementService settlementService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void process(final Long inboxId) {
        final SettlementInbox inbox = settlementInboxJpaRepository.findById(inboxId)
                .orElse(null);

        // 재전송된 메시지를 다시 처리하지 않는다.
        if (inbox == null || !inbox.isPending()) {
            return;
        }

        final FinishedOrderEvent event =
                objectMapper.readValue(inbox.getPayload(), FinishedOrderEvent.class);

        // 같은 주문이 두 번 들어와도 SettlementService 가 purchaseId / offerId 로 중복을 걸러낸다.
        createSettlement(event);

        inbox.markAsProcessed();
    }

    private void createSettlement(final FinishedOrderEvent event) {
        InternalCallContext.setMemberId(String.valueOf(event.sellerId()));

        try {
            settlementService.createSettlement(event);
        } finally {
            InternalCallContext.clear();
        }
    }

    @Transactional
    public void markFailed(final Long inboxId, final String reason) {
        settlementInboxJpaRepository.findById(inboxId)
                .ifPresent(inbox -> inbox.markAsFailed(reason));
    }
}
