package shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.SettlementInbox;
import shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox.SettlementInboxJdbcRepository;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementInboxService {

    private final SettlementInboxJdbcRepository settlementInboxJdbcRepository;

    @Transactional
    public Optional<Long> save(final SettlementInbox inbox) {
        final Optional<Long> inboxId = settlementInboxJdbcRepository.insertIgnoringDuplicate(inbox);

        if (inboxId.isEmpty()) {
            log.info("[SettlementInbox] 이미 적재된 이벤트를 건너뜁니다. eventId={}", inbox.getEventId());
            return inboxId;
        }

        log.info(
                "[SettlementInbox] 적재 완료. id={}, eventId={}, eventType={}, aggregateId={}, status={}",
                inboxId.get(),
                inbox.getEventId(),
                inbox.getEventType(),
                inbox.getAggregateId(),
                inbox.getStatus()
        );

        return inboxId;
    }
}
