package shop.dear.recommendation.infrastructure.inbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboxService {

    private final RecommendationInboxJpaRepository recommendationInboxJpaRepository;

    @Transactional
    public void saveProductEvents(final List<RecommendationInbox> inboxes) {
        if (inboxes.isEmpty()) {
            return;
        }

        final List<Long> eventIds = inboxes.stream()
            .map(RecommendationInbox::getEventId)
            .toList();

        final Set<Long> alreadySaved =
            Set.copyOf(recommendationInboxJpaRepository.findEventIdsIn(eventIds));

        //이미 저장된 event인지 검증 (중복방지)
        final List<RecommendationInbox> newInboxes = inboxes.stream()
            .filter(inbox -> !alreadySaved.contains(inbox.getEventId()))
            .toList();

        recommendationInboxJpaRepository.saveAll(newInboxes);

        log.info("[InboxService] inbox 적재 완료. received={}, saved={}, skipped={}",
            inboxes.size(), newInboxes.size(), inboxes.size() - newInboxes.size());
    }
}
