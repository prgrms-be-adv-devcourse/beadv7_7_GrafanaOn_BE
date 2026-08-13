package shop.dear.commerce.search.infrastructure.elasticsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;
import shop.dear.commerce.search.application.port.SearchIndexRebuilder;
import shop.dear.commerce.search.application.port.dto.ReindexResult;
import shop.dear.commerce.search.domain.exception.SearchErrorCode;
import shop.dear.commerce.search.infrastructure.SearchJpaRepository;
import shop.dear.commerce.search.infrastructure.SearchProductJpaEntity;
import shop.dear.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * search_product 테이블을 읽어 Elasticsearch로 색인한다.
 * search.engine의 설정과 무관하게 동작합니다.
 *
 * 새 인덱스를 만들어 색인한 뒤 별칭만 옮기는 방식입니다.
 * 구 인덱스가 완성될 때까지 계속 서비스하므로 재구축 중에도 검색이 끊기지 않고, 원본에 없는 고아 문서도 함께 사라집니다.
 *
 * search_product DB -> 신규 ES 인덱스 생성
 * -> DB 데이터를 1,000건씩 신규 인덱스에 저장 -> search_product 별칭을 신규 인덱스로 이동
 * -> 재색인 중 변경된 데이터 보정 -> 기존 ES 인덱스 삭제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsSearchIndexRebuilder implements SearchIndexRebuilder {

    private static final int CHUNK_SIZE = 1000;
    private static final String INDEX_NAME_PREFIX = SearchProductDocument.INDEX_ALIAS + "_";
    private static final DateTimeFormatter INDEX_SUFFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    // 동시 실행을 막는다. 두 재색인이 서로의 별칭을 옮기고 인덱스를 지울 수 있는 문제 발생 위험.
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private final SearchJpaRepository jpaRepository;
    private final ElasticsearchOperations operations;

    @Override
    public ReindexResult rebuild() {
        if (!RUNNING.compareAndSet(false, true)) {
            throw new BusinessException(SearchErrorCode.REINDEX_ALREADY_RUNNING);
        }

        try {
            return doRebuild();
        } finally{
            RUNNING.set(false); // 영구적으로 잠기는 문제 방지
        }
    }

    private ReindexResult doRebuild() {
        final LocalDateTime startedAt = LocalDateTime.now();
        final String newIndexName = INDEX_NAME_PREFIX + startedAt.format(INDEX_SUFFIX_FORMATTER);
        final IndexCoordinates newIndex = IndexCoordinates.of(newIndexName);

        // nori 분석기, 필드 타입 등 SearchProductDocument의 매핑을 이용해 빈 인덱스 생성.
        createIndex(newIndexName);

        try {
            long sourceCount = jpaRepository.count(); // search_product 테이블 소스 개수
            long indexedCount = indexAll(newIndex);

            // 강제 갱신
            operations.indexOps(IndexCoordinates.of(newIndexName)).refresh();

            // 별칭을 옮기기 전의 인덱스 목록을 받아둔다.
            Set<String> previousIndices = switchAlias(newIndexName);

        /*
          별칭을 옮긴 뒤에 보정한다. 옮기기 전까지 실시간 색인은 구 인덱스로 들어간다.
          따라서 그 사이에 추가된 정보들은 신규 인덱스에 없다.
          별칭 이동 후에는 실시간 색인도 신규 인덱스로 오므로 여기서 메워 누락이 남지 않게 한다.
        */
            long deltaCount = indexDelta(startedAt);
            operations.indexOps(newIndex).refresh();

            // 기존 인덱스 삭제
            deleteIndices(previousIndices);

            long documentCount = operations.count(Query.findAll(), SearchProductDocument.class);

            log.info("재색인 완료. 인덱스 = {} 원본 = {} 색인 = {} 보정 = {} 문서 = {}", newIndexName, sourceCount, indexedCount, deltaCount, documentCount);

            return new ReindexResult(sourceCount, indexedCount, documentCount);
        } catch (RuntimeException e) {
            // 실패한 재색인이 만든 인덱스는 별칭이 가리키지 않아 이후 정리 대상이 되지 않는다.
            deleteIndices(Set.of(newIndexName));
            throw e;
        }
    }

    /**
     * 인덱스 없이 색인하면 ES가 추측으로 동적 매핑으로 만들어버려 nori 적용이 안될 수 있습니다.
     * 신규 인덱스를 문서 클래스의 설정과 매핑 그대로 생성합니다.
     */
    private void createIndex(final String newIndexName) {

        // 작업 객체 가져오기
        final IndexOperations documentOps = operations.indexOps(SearchProductDocument.class);

        // 신규 인덱스에 Nori, text, keyword 필드, 날짜 형식, 숫자 타입, shard & replica 설정
        operations.indexOps(IndexCoordinates.of(newIndexName)).create(
                documentOps.createSettings(SearchProductDocument.class),
                documentOps.createMapping(SearchProductDocument.class)
        );

        log.info("신규 인덱스를 생성했습니다. index = {}", newIndexName);
    }

    private long indexAll(final IndexCoordinates target) {

        long indexedCount = 0; // 지금까지 색인한 문서 수 // 마지막으로 읽은 상품 ID
        long lastProductId = 0;

        while (true) {
            // 키셋 페이지네이션
            final List<SearchProductJpaEntity> entities = jpaRepository.findNextChunk(lastProductId, Limit.of(CHUNK_SIZE));

            if (entities.isEmpty()) {
                break;
            }

            // 신규 인덱스로 직접 보낸다.
            operations.save(toDocuments(entities), target);

            indexedCount += entities.size();
            lastProductId = entities.get(entities.size() - 1).getProductId();

            log.info("색인 진행 {}건 (마지막 id = {})", indexedCount, lastProductId);
        }

        return indexedCount;
    }

    /**
     * @param newIndexName : 신규 인덱스 이름
     * @return : 별칭 이동 전에 별칭이 가리키던 인덱스, 최초 도입 시에는 비어 있습니다.
     */
    private Set<String> switchAlias(final String newIndexName) {
        final IndexCoordinates alias = IndexCoordinates.of(SearchProductDocument.INDEX_ALIAS);
        final IndexOperations aliasOps = operations.indexOps(alias);
        final Set<String> previousIndices = findIndicesBehindAlias();
        final AliasActions actions = new AliasActions();

        if (previousIndices.isEmpty() && aliasOps.exists()) {
            /* 별칭은 없는데 같은 이름의 실제 인덱스가 있는 상황. 별칭 도입 이전에 만들어진 것.
               인덱스와 별칭은 이름이 같을 수 없으니 삭제해야 한다.
               삭제와 별칭 추가를 나눠서 하면 그 사이 실시간 색인 요청이 같은 이름의 인덱스를 자동 생성해버려 별칭 추가에 실패할 수 있다.
               한 요청으로 묶어 원자적으로 처리하여 이 문제를 해결한다.
             */
            actions.add(new AliasAction.RemoveIndex(AliasActionParameters.builder()
                    .withIndices(SearchProductDocument.INDEX_ALIAS)
                    .build()));
        }

        if(!previousIndices.isEmpty()) {
            actions.add(new AliasAction.Remove(AliasActionParameters.builder()
                    .withIndices(previousIndices.toArray(String[]::new))
                    .withAliases(SearchProductDocument.INDEX_ALIAS)
                    .build()));
        }

        actions.add(new AliasAction.Add(AliasActionParameters.builder()
                .withIndices(newIndexName)
                .withAliases(SearchProductDocument.INDEX_ALIAS)
                .build()));

        aliasOps.alias(actions);

        log.info("별칭을 이동했습니다. {} -> {}", previousIndices, newIndexName);

        return previousIndices;
    }

    private Set<String> findIndicesBehindAlias() {
        try {
            return operations.indexOps(IndexCoordinates.of(SearchProductDocument.INDEX_ALIAS))
                    .getAliases(SearchProductDocument.INDEX_ALIAS)
                    .keySet();
        } catch (ResourceNotFoundException e) {
            // 별칭이 아직 없으면 ES가 404를 주고 Spring Data가 예외로 바꾼다.
            // 최초 재색인에서는 정상 상황이므로 빈 집합으로 다룬다.
            return Set.of();
        }
    }


    // 재색인을 시작한 이후 수정되거나 추가된 데이터를 찾는다.
    private long indexDelta(LocalDateTime startedAt) {
        List<SearchProductJpaEntity> changed = jpaRepository.findAllByUpdatedAtGreaterThanEqual(startedAt);

        if (changed.isEmpty()) {
            return 0;
        }

        operations.save(toDocuments(changed));
        log.info("재색인 도중 변경된 {}건을 보정했습니다.", changed.size());

        return changed.size();
    }

    private void deleteIndices(Set<String> indexNames) {
        // 별칭이 이미 신규 인덱스를 가리키므로 실패해도 서비스에는 영향이 없다.
        indexNames.forEach(indexName -> {
            try {
                operations.indexOps(IndexCoordinates.of(indexName)).delete();
                log.info("이전 인덱스를 삭제했습니다. index = {}", indexName);
            } catch (Exception e) {
                log.warn("이전 인덱스 삭제에 실패했습니다. 수동 정리가 필요합니다. index = {}", indexName, e);
            }
        });
    }

    private List<SearchProductDocument> toDocuments(List<SearchProductJpaEntity> entities) {
        return entities.stream()
                .map(entity -> SearchProductDocument.from(entity.toDomain()))
                .toList();
    }
}
