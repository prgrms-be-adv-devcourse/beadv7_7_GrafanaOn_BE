package shop.dear.commerce.search.presentation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import shop.dear.commerce.search.presentation.dto.SearchPageResponse;
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
     * 카테고리 검색: /api/search/products?keyword=SNEAKERS&type=CATEGORY&page=0&size=20
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<SearchPageResponse>> searchProducts(
            @RequestParam
            @NotBlank(message = "검색어를 입력해주세요.")
            String keyword,

            // 검색 타입 기본 값은 상품명 검색
            @RequestParam(defaultValue = "PRODUCT_NAME")
            SearchType type,

            // Spring 내에서 페이지 번호는 1번부터 시작한다.
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
            int size,

            @RequestParam(defaultValue = "LATEST")
            SearchSort sort
            ) {
        SearchQuery query = new SearchQuery(
                keyword,
                type,
                sort,
                page - 1, // 실제로는 0번부터 시작한다.
                size
        );

        SearchPageResponse response = SearchPageResponse
                .from(searchService.search(query));

        return ResponseEntity.ok(successWithData(response));
    }
}
