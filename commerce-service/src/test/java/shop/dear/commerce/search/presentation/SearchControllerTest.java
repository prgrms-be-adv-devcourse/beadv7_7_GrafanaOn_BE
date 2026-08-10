package shop.dear.commerce.search.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.dear.commerce.search.application.SearchService;
import shop.dear.commerce.search.application.dto.SearchQuery;
import shop.dear.commerce.search.application.dto.SearchResult;
import shop.dear.commerce.search.application.dto.SearchSort;
import shop.dear.commerce.search.application.dto.SearchType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 0쪽 입력을 1쪽 출력으로 잘 반환하는지?
@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @Test
    void searchesProductsWithDefaultOptions() throws Exception {
        SearchResult searchResult = new SearchResult(
                "나이키 에어포스",
                "CW2288-111",
                "SNEAKERS",
                LocalDate.of(2025, 1, 1),
                new BigDecimal("139000"),
                "IMMEDIATE",
                100L,
                "화이트 운동화"
        );

        given(searchService.search(any(SearchQuery.class)))
                .willReturn(new PageImpl<>(
                        List.of(searchResult),
                        PageRequest.of(0, 20),
                        1
                ));

        mockMvc.perform(get("/api/search/products")
                        .param("keyword", "나이키")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("success"))
                .andExpect(jsonPath("$.data.pagination.currentPage").value(1))
                .andExpect(jsonPath("$.data.pagination.pageSize").value(20))
                .andExpect(jsonPath("$.data.pagination.totalItems").value(1))
                .andExpect(jsonPath("$.data.content[0].productName")
                        .value("나이키 에어포스"))
                .andExpect(jsonPath("$.data.content[0].category")
                        .value("SNEAKERS"));

        verify(searchService).search(new SearchQuery(
                "나이키",
                SearchType.PRODUCT_NAME,
                SearchSort.LATEST,
                0,
                20
        ));
    }

    // page=0을 요청하면 PaginationRequest가 기본 페이지(1)로 정규화한다
    @Test
    void normalizesPageNumberLessThanOneToDefault() throws Exception {
        given(searchService.search(any(SearchQuery.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/search/products")
                        .param("keyword", "나이키")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("success"));

        verify(searchService).search(new SearchQuery(
                "나이키",
                SearchType.PRODUCT_NAME,
                SearchSort.LATEST,
                0,
                20
        ));
    }
}