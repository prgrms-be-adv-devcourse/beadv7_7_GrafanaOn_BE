package shop.dear.commerce.search.presentation;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.commerce.search.application.SearchService;
import shop.dear.commerce.search.application.dto.SearchQuery;
import shop.dear.commerce.search.application.dto.SearchResult;
import shop.dear.commerce.search.application.dto.SearchSort;
import shop.dear.commerce.search.application.dto.SearchType;
import shop.dear.common.pagination.PaginationRequest;
import shop.dear.common.pagination.PaginationResponse;
import shop.dear.common.response.ApiResponse;

import static shop.dear.common.response.ApiResponse.successWithData;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;

    /**
     * Gateway를 거칩니다. 외부 클라이언트가 각 서비스 주소를 직접 알게 하지 않습니다.
     * 기본 검색 : /api/search/products?keyword=나이키&page=1&size=20
     * 카테고리 검색: /api/search/products?keyword=SNEAKERS&type=CATEGORY&page=1&size=20
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<PaginationResponse<SearchResult>>> searchProducts(
            @RequestParam @NotBlank(message = "검색어를 입력해주세요.") String keyword,
            // 검색 타입 기본 값은 상품명 검색
            @RequestParam(defaultValue = "PRODUCT_NAME") SearchType type,
            // 외부 API의 페이지 번호는 1부터 시작한다.
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,

            @RequestParam(defaultValue = "LATEST")
            SearchSort sort
            ) {
        PaginationRequest paginationRequest = new PaginationRequest(page, size, 20, 100);

        SearchQuery query = new SearchQuery(
                keyword,
                type,
                sort,
                paginationRequest.getPageNo() - 1, // 실제로는 0번부터 시작한다.
                paginationRequest.getPageSize()
        );

        Page<SearchResult> result = searchService.search(query);

        return ResponseEntity.ok(successWithData(PaginationResponse.of(result, result.getContent())));
    }
}
