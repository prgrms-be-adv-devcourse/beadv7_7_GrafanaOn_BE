package shop.dear.commerce.search.infrastructure.elasticsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

/**
 * 인덱스가 없는 상태에서 색인하면 ES가 동적 매핑으로 인덱스를 자동 생성하는데,
 * 이 경우 nori 분석기와 keyword 지정이 모두 사라지므로 한글 검색이 망가진다.
 *
 * ES가 꺼져 있어도 서비스는 기동되어야 하므로 실패는 로그만 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
// ApplicationRunner를 구현하면 Spring 시작 시 run() 자동 호출
public class SearchIndexInitializer implements ApplicationRunner {

    private final ElasticsearchOperations operations;

    @Override
    public void run(ApplicationArguments args) {
        try {
            IndexOperations aliasOps = operations.indexOps(IndexCoordinates.of(SearchProductDocument.INDEX_ALIAS));

            // 인덱스가 이미 존재하면 종료.
            if (aliasOps.exists()) {
                return;
            }

            log.warn("search_product 별칭이 없습니다. POST - reindex API를 실행하세요");
        } catch (Exception e) {
            log.warn("search_product 인덱스 상태 확인에 실패했습니다. ES 상태를 확인하세요.");
        }
    }
}
