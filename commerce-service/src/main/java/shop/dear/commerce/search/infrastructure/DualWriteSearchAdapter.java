package shop.dear.commerce.search.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.search.domain.SearchProduct;
import shop.dear.commerce.search.domain.SearchRepository;
import shop.dear.commerce.search.infrastructure.elasticsearch.EsSearchAdapter;

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
        return esAdapter.searchByProductName(keyword, pageable);
    }

    @Override
    public Page<SearchProduct> searchByCategory(String category, Pageable pageable) {
        return esAdapter.searchByCategory(category, pageable);
    }

    @Override
    public Page<SearchProduct> searchByStoryContent(String keyword, Pageable pageable) {
        return esAdapter.searchByStoryContent(keyword, pageable);
    }
}
