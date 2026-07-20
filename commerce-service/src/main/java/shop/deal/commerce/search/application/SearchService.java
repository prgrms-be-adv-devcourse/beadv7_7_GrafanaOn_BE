package shop.deal.commerce.search.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.deal.commerce.search.application.dto.SearchQuery;
import shop.deal.commerce.search.application.dto.SearchResult;
import shop.deal.commerce.search.application.dto.SearchSort;
import shop.deal.commerce.search.domain.SearchRepository;

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

        return searchRepository.searchByProductName(query.keyword(), pageable);
    }

    private Sort toSort(SearchSort sort) {
        // 기본 값은 최신순으로 진행.
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "insertedAt");
        }

        // 최신 순, 조회수 높은 순, 가격 낮은 순, 가격 높은 순
        return switch (sort) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "insertedAt");
            case VIEW_COUNT -> Sort.by(Sort.Direction.DESC, "viewCount");
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "price");
        };
    }
}