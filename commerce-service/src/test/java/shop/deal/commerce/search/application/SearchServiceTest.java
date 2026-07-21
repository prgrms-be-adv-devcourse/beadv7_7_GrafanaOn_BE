package shop.deal.commerce.search.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import shop.deal.commerce.search.application.dto.SearchType;
import shop.deal.commerce.search.application.dto.SearchQuery;
import shop.deal.commerce.search.application.dto.SearchResult;
import shop.deal.commerce.search.application.dto.SearchSort;
import shop.deal.commerce.search.domain.SearchProduct;
import shop.deal.commerce.search.domain.SearchRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

// 테스트 코드. Made By Codex.
@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {

    @Mock
    private SearchRepository searchRepository;

    @InjectMocks
    private SearchService searchService;

    @Test
    void searchesProductsByProductName() {
        SearchQuery query = new SearchQuery(
                "나이키",
                SearchType.PRODUCT_NAME,
                SearchSort.VIEW_COUNT,
                0,
                20
        );

        SearchProduct product = new SearchProduct(
                1L,
                "나이키 에어포스",
                "CW2288-111",
                "SNEAKERS",
                LocalDate.of(2025, 1, 1),
                new BigDecimal("139000"),
                "IMMEDIATE",
                100L,
                "화이트 운동화",
                "꽁꽁 숨겨져 있는 어느 빈티지샵에서 발굴했습니다.",
                LocalDateTime.of(2025, 1, 1, 12, 0)
        );

        given(searchRepository.searchByProductName(
                eq("나이키"),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(product)));

        Page<SearchResult> result = searchService.search(query);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).productName())
                .isEqualTo("나이키 에어포스");

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(searchRepository).searchByProductName(
                eq("나이키"),
                pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("viewCount")
                .getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void searchesProductsByCategory() {
        SearchQuery query = new SearchQuery(
                "SNEAKERS",
                SearchType.CATEGORY,
                SearchSort.LATEST,
                0,
                20
        );

        given(searchRepository.searchByCategory(
                eq("SNEAKERS"),
                any(Pageable.class)
        )).willReturn(Page.empty());

        searchService.search(query);

        verify(searchRepository).searchByCategory(
                eq("SNEAKERS"),
                any(Pageable.class)
        );
    }

    @Test
    void searchesProductsByStoryContent() {
        SearchQuery query = new SearchQuery(
                "빈티지샵",
                SearchType.STORY_CONTENT,
                SearchSort.LATEST,
                0,
                20
        );

        given(searchRepository.searchByStoryContent(
                eq("빈티지샵"),
                any(Pageable.class)
        )).willReturn(Page.empty());

        searchService.search(query);

        verify(searchRepository).searchByStoryContent(
                eq("빈티지샵"),
                any(Pageable.class)
        );
    }
}
