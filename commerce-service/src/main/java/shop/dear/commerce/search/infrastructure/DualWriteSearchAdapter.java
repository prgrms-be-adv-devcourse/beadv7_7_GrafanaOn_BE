package shop.dear.commerce.search.infrastructure;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.micrometer.core.instrument.search.Search;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.search.domain.SearchProduct;
import shop.dear.commerce.search.domain.SearchRepository;
import shop.dear.commerce.search.domain.exception.SearchErrorCode;
import shop.dear.commerce.search.infrastructure.elasticsearch.EsSearchAdapter;
import shop.dear.common.exception.ServiceUnavailableException;

import java.util.function.Supplier;

/**
 * 쓰기는 search_product 테이블과 Elasticsearch 양쪽에, 조회는 Elasticsearch로 위임한다.
 * search_product는 재색인의 원본이다.
 * ES 인덱스가 유실되거나 어긋아면 이 테이블을 읽어 다시 만들기 때문에, 항상 최신이어야 한다.
 * 즉, ES 쓰기가 실패해도 테이블 기록은 남아야 한다.
 * 불일치는 감지 지표와 재색인으로 해결한다.
 */
@Slf4j
@Primary
@Repository
@RequiredArgsConstructor
public class DualWriteSearchAdapter implements SearchRepository {

    private final SearchRepositoryAdapter jpaAdapter;
    private final EsSearchAdapter esAdapter;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final Bulkhead jpaSearchFallbackBulkhead;

    @Override
    public void save(SearchProduct product) {
        jpaAdapter.save(product);

        try {
            esAdapter.save(product);
        } catch (Exception e) {
            log.warn("검색 인덱스 반영에 실패했습니다. 재색인이 필요합니다. productID = {}", product.getProductId(), e);
        }
    }

    @Override
    public void deleteByProductId(Long productId) {
        jpaAdapter.deleteByProductId(productId);

        try {
            esAdapter.deleteByProductId(productId);
        } catch (Exception e) {
            log.warn("검색 인덱스 삭제에 실패했습니다. 재색인이 필요합니다. productId = {}", productId, e);
        }
    }

    @Override
    public Page<SearchProduct> searchByProductName(String keyword, Pageable pageable) {
        return searchWithFallback(
                () -> esAdapter.searchByProductName(keyword, pageable),
                () -> jpaAdapter.searchByProductName(keyword, pageable)
        );
    }

    @Override
    public Page<SearchProduct> searchByCategory(String category, Pageable pageable) {
        return searchWithFallback(
                () -> esAdapter.searchByCategory(category, pageable),
                () -> jpaAdapter.searchByCategory(category, pageable)
        );
    }

    @Override
    public Page<SearchProduct> searchByStoryContent(String keyword, Pageable pageable) {
        return searchWithFallback(
                () -> esAdapter.searchByStoryContent(keyword, pageable),
                () -> jpaAdapter.searchByStoryContent(keyword, pageable)
        );
    }

    private Page<SearchProduct> searchWithFallback(final Supplier<Page<SearchProduct>> esSearch, final Supplier<Page<SearchProduct>> jpaSearch) {
        return circuitBreakerFactory.create(SearchResilienceConfig.ELASTICSEARCH)
                .run(esSearch, throwable -> fallbackToJpa(throwable, jpaSearch));
    }

    private Page<SearchProduct> fallbackToJpa(final Throwable cause, final Supplier<Page<SearchProduct>> jpaSearch) {
        log.warn("Elasticsearch 조회에 실패해 JPA 검색으로 전환합니다. 원인: {}", cause.toString());

        try {
            return Bulkhead.decorateSupplier(jpaSearchFallbackBulkhead, jpaSearch).get();
        } catch (final BulkheadFullException exception) {
            // 기다리게 하지 않고 즉시 실패시킨다.
            log.warn("JPA 검색 동시 실행 한도를 초과했습니다.");

            throw new ServiceUnavailableException(SearchErrorCode.SEARCH_TEMPORARILY_UNAVAILABLE);
        }
    }
}
