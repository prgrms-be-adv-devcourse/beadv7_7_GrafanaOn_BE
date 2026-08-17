package shop.dear.commerce.search.infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.search.domain.SearchProduct;
import shop.dear.commerce.search.domain.SearchRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EsSearchAdapter implements SearchRepository {

    private final ElasticsearchOperations operations;

    @Override
    public void save(SearchProduct product) {
        operations.save(SearchProductDocument.from(product));
    }

    @Override
    public void deleteByProductId(Long productId) {
        operations.delete(String.valueOf(productId), SearchProductDocument.class);

    }

    @Override
    public Page<SearchProduct> searchByProductName(String keyword, Pageable pageable) {
        return search(
                NativeQuery.builder()
                        .withQuery(q -> q.match(m -> m
                                .field("name")
                                .query(keyword)
                                .operator(Operator.And)
                        )),
                pageable
        );
    }

    @Override
    public Page<SearchProduct> searchByCategory(String category, Pageable pageable) {
        return search(
                NativeQuery.builder()
                        .withQuery(q -> q.term(t -> t
                                .field("category")
                                .value(category)
                                .caseInsensitive(true)
                        )),
                pageable
        );
    }

    @Override
    public Page<SearchProduct> searchByStoryContent(String keyword, Pageable pageable) {
        return search(
                NativeQuery.builder()
                        .withQuery(q -> q.match(m -> m
                                .field("story_content")
                                .query(keyword)
                                .operator(Operator.And)
                        )),
                pageable
        );
    }

    /**
     * Elasticsearch 검색 조건을 실행하고, 결과를 도메인 객체의 Page로 변환한다.
     * @param builder: 상품명, 카테고리 등의 검색 조건이 들어있는 ES 쿼리
     * @param pageable: 조회할 페이지 번호, 크기, 정렬 정보
     * @return: 검색 결과와 페이지 정보를 가진 Page<SearchProduct>
     */
    private Page<SearchProduct> search(NativeQueryBuilder builder,Pageable pageable) {
        NativeQuery query = builder
                .withPageable(pageable) // 페이징 정보 추가
                .withTrackTotalHits(true) // ES에서는 기본적으로 10,000건에서 끊는다. 정확한 전체 검색 개수 측정.
                .build();

        // 완성된 쿼리를 Elasticsearch에 전송한다.
        SearchHits<SearchProductDocument> hits = operations.search(query, SearchProductDocument.class);
        // 검색 결과와 부가 정보들에서 content에 검색 결과 목록만 빼낸다.
        List<SearchProduct> content = hits.getSearchHits().stream()
                .map(hit -> hit.getContent().toDomain())
                .toList();

        // Spring의 Page 객체로 포장하여 반환
        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }
}
