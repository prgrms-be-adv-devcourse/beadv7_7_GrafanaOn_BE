package shop.dear.commerce.search.infrastructure.elasticsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;
import shop.dear.commerce.search.application.port.SearchIndexRebuilder;
import shop.dear.commerce.search.application.port.dto.ReindexResult;
import shop.dear.commerce.search.infrastructure.SearchJpaRepository;
import shop.dear.commerce.search.infrastructure.SearchProductJpaEntity;

import java.util.List;

/**
 * search_product 테이블을 읽어 Elasticsearch로 색인한다.
 *
 * search.engine의 설정과 무관하게 동작합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsSearchIndexRebuilder implements SearchIndexRebuilder {

    private static final int CHUNK_SIZE = 1000;

    private final SearchJpaRepository jpaRepository;
    private final ElasticsearchOperations operations;

    @Override
    public ReindexResult rebuild() {
        // 작업 객체 가져오기
        IndexOperations indexOperations = operations.indexOps(SearchProductDocument.class);

        // 인덱스 없이 색인하면 ES가 추측으로 동적 매핑으로 만들어버려 nori 적용이 안될 수 있습니다.
        // 만약 없다면 매핑과 함께 생성합니다.
        if (!indexOperations.exists()) {
            indexOperations.createWithMapping();
            log.info("search_product 인덱스를 생성했습니다.");
        }

        long indexedCount = 0; // 지금까지 ES에 보낸 문서 수
        int pageNumber = 0; // 현재 읽을 JPA 페이지
        Page<SearchProductJpaEntity> page; // 이번에 조회한 데이터와 전체 개수, 다음 페이지 정보를 보관

        do {
            // id 기준으로 정렬하며, JPA 데이터를 페이지 단위로 끊어 읽는다.
            page = jpaRepository.findAll(
                    PageRequest.of(pageNumber, CHUNK_SIZE, Sort.by(Sort.Direction.ASC, "productId"))
            );

            // JPA Entity를 ES Document로 변환한다.
            // SearchProductJpaEntity -> SearchProduct -> SearchProductDocument
            List<SearchProductDocument> documents = page.getContent().stream()
                    .map(entity -> SearchProductDocument.from(entity.toDomain()))
                    .toList();

            // 현재 페이지의 문서를 Elasticsearch에 한 번에 저장한다.
            if (!documents.isEmpty()) {
                operations.save(documents);
                indexedCount += documents.size();
                log.info("색인 진행 {} / {}", indexedCount, page.getTotalElements());
            }

            pageNumber++;
        } while (page.hasNext()); // 이를 다음 페이지가 있을 때까지 반복한다.

        // 강제 갱신
        indexOperations.refresh();

        long documentCount = operations.count(Query.findAll(), SearchProductDocument.class);

        // 정상이라면 모두 같은 숫자가 나와야 한다.
        // TODO: 다만 현재는 기존 ES를 지우지 않고 upsert만 하므로 상품 삭제 이벤트가 발생하면 달라질 수 있다.
        log.info("재색인 완료. 원본 = {} 색인 = {} 문서 = {}", page.getTotalElements(), indexedCount, documentCount);

        return new ReindexResult(page.getTotalElements(), indexedCount, documentCount);
    }
}
