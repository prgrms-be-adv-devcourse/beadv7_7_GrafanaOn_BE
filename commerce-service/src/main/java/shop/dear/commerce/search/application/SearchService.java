package shop.dear.commerce.search.application;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.search.application.dto.SearchQuery;
import shop.dear.commerce.search.application.dto.SearchResult;
import shop.dear.commerce.search.application.dto.SearchSort;
import shop.dear.commerce.search.application.dto.SearchType;
import shop.dear.commerce.search.domain.SearchProduct;
import shop.dear.commerce.search.domain.SearchRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 검색은 읽기 전용
public class SearchService {

    private final SearchRepository searchRepository;

    public Page<SearchResult> search(SearchQuery query) {
        Pageable pageable = PageRequest.of(
                query.page(),
                query.size(),
                toSort(query.sort())
        );

        SearchType type = query.type() == null
                ? SearchType.PRODUCT_NAME // 기본 값은 상품명 검색
                : query.type();

        Page<SearchProduct> products = switch (type) {
            case PRODUCT_NAME -> searchRepository.searchByProductName(
                    query.keyword(),
                    pageable
            );

            case CATEGORY -> searchRepository.searchByCategory(
                    query.keyword(),
                    pageable
            );

            case STORY_CONTENT -> searchRepository.searchByStoryContent(
                    query.keyword(),
                    pageable
            );
        };

        return products.map(this::toSearchResult);
    }

    private SearchResult toSearchResult(SearchProduct product) {
        return new SearchResult(
                product.getProductName(),
                product.getModelNumber(),
                product.getCategory(),
                product.getReleaseDate(),
                product.getProductPrice(),
                product.getSaleType(),
                product.getViewCount(),
                product.getDescription()
        );
    }


    private Sort toSort(SearchSort sort) {
        // 기본 값은 최신순으로 진행.
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "productInsertedAt");
        }

        // 최신 순, 조회수 높은 순, 가격 낮은 순, 가격 높은 순
        return switch (sort) {

            // Spring  Data Elasticsearch가 "_score"를 필드가 아닌 스코어 정렬로 변환해준다.
            case RELEVANCE -> Sort.by(Sort.Direction.DESC, "_score");
            case LATEST -> Sort.by(Sort.Direction.DESC, "productInsertedAt");
            case VIEW_COUNT -> Sort.by(Sort.Direction.DESC, "viewCount");
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "productPrice");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "productPrice");
        };
    }
}