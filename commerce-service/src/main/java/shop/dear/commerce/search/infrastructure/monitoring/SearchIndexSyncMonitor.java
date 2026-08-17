package shop.dear.commerce.search.infrastructure.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shop.dear.commerce.search.infrastructure.SearchJpaRepository;
import shop.dear.commerce.search.infrastructure.elasticsearch.SearchProductDocument;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 검색 원본 테이블의 상품 수와 Elasticsearch 문서 수를 5분마다 비교하고,
 * 그 결과를 Prometheus 메트릭으로 노출하는 모니터링 컴포넌트
 * 무엇을 측정할 지 결정하는 책임은 Search에 있고, 측정값을 수집 및 저장하는 책임은 모니터링에 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexSyncMonitor {

    // 측정값 3개
    private final AtomicLong sourceCount = new AtomicLong(-1); // 색인된 원본 건수
    private final AtomicLong documentCount = new AtomicLong(-1); // ES 인덱스에 실제로 들어있는 문서 수
    private final AtomicLong countDiff = new AtomicLong(-1); // 위 둘의 차이. 0이 아닐 시 문제 발생.

    private final SearchJpaRepository searchJpaRepository;
    private final ElasticsearchOperations operations;
    private final MeterRegistry meterRegistry; // 메트릭 등록

    @PostConstruct // 이 Bean의 의존성 주입이 끝난 직후 한 번만 실행하라.
    // 다음 세 개의값들이 Prometheus로 노출될 것이다.
    void registerGauges() {
        meterRegistry.gauge("search.index.source.count", sourceCount);
        meterRegistry.gauge("search.index.document.count", documentCount);
        meterRegistry.gauge("search.index.count.diff", countDiff);
    }

    // 이전 실행이 끝난 뒤 5분을 대기.
    @Scheduled(fixedDelay = 300_000)
    public void checkSync() {
        try {
            final long source = searchJpaRepository.count();
            final long documents = operations.count(Query.findAll(), SearchProductDocument.class);
            final long diff = Math.abs(source - documents);

            sourceCount.set(source);
            documentCount.set(documents);
            countDiff.set(diff);

            if (diff > 0) {
                log.warn("검색 인덱스 불일치. 원본 = {} 문서 = {} 차이 = {}", source, documents, diff);
            }
        } catch (Exception e) {
            // ES가 꺼져 있어도 서비스는 계속 동작해야 한다. 따라서 로그만 남긴다.
            log.warn("검색 인덱스 동기화 확인에 실패했습니다.", e);
        }
    }
}
