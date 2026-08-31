package shop.dear.commerce.financial.settlement.infrastructure.messaging.inbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementInboxService {

    private final SettlementInboxJdbcRepository settlementInboxJdbcRepository;

    @Transactional
    public InboxSaveResult save(final SettlementInbox inbox) {
        final InboxSaveResult saved = settlementInboxJdbcRepository.insertOrGetExisting(inbox);

        log.info(
                "[SettlementInbox] 적재 결과. id={}, eventId={}, eventType={}, aggregateId={}, status={}",
                saved.id(),
                inbox.getEventId(),
                inbox.getEventType(),
                inbox.getAggregateId(),
                saved.status()
        );

        return saved;
    }
}
