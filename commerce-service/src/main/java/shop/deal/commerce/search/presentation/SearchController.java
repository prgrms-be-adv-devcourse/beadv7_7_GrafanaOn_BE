package shop.deal.commerce.search.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shop.deal.commerce.search.application.SearchService;
import shop.deal.commerce.search.application.dto.SearchQuery;
import shop.deal.commerce.search.application.dto.SearchResult;
import shop.deal.commerce.search.application.dto.SearchSort;

@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
public class SearchController {
    private final SearchService searchService;

    // 예시 : /search/products?keyword=나이키&page=0&size=20
    // /search/products?keyword=나이키&page=0&size=20&sort=VIEW_COUNT
    @GetMapping("/products")
    public ResponseEntity<Page<SearchResult>> searchProducts(
            @RequestParam String keyword,
            // Spring 내에서 페이지 번호는 0번부터 시작한다.
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "LATEST") SearchSort sort
            ) {
        SearchQuery query = new SearchQuery(
                keyword,
                sort,
                page,
                size
        );

        return ResponseEntity.ok(searchService.search(query));
    }
}
