package shop.dear.recommendation.infrastructure.inbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboxService {

    private final RecommendationInboxJdbcRepository recommendationInboxJdbcRepository;

    @Transactional
    public void saveProductEvents(final List<RecommendationInbox> inboxes) {
        if (inboxes.isEmpty()) {
            return;
        }

        final LocalDateTime now = LocalDateTime.now();

        final int saved = recommendationInboxJdbcRepository.insertIgnoringDuplicate(now, inboxes);

        log.info("[InboxService] inbox 적재 완료. received={}, saved={}, skipped={}",
            inboxes.size(), saved, inboxes.size() - saved);
    }
}
