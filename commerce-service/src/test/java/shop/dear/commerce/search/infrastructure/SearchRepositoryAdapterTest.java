package shop.dear.commerce.search.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.search.domain.SearchProduct;
import shop.dear.commerce.search.domain.SearchRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 1. 검색어: 에어포스 / 결과: 나이키 에어포스 1개 조회
 * 2. 카테고리 필터 검색: SNEAKERS / 결과: SNEAKERS 상품 1개 조회
 * 3. 스토리 내용 검색: 빈티지샵 / 결과 : 해당 스토리르 가진 상품 1개 조회
 */
@SpringBootTest
@Transactional
class SearchRepositoryAdapterTest {

    @Autowired
    private SearchRepository searchRepository;

    @Test
    void searchesByProductNameContainingKeyword() {
        searchRepository.save(createProduct(
                1L,
                "나이키 에어포스",
                "SNEAKERS",
                "빈티지샵에서 구매한 상품"
        ));

        Page<SearchProduct> result = searchRepository.searchByProductName(
                "에어포스",
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductName())
                .isEqualTo("나이키 에어포스");
    }

    @Test
    void searchesByCategoryIgnoringCase() {
        searchRepository.save(createProduct(
                1L,
                "나이키 에어포스",
                "SNEAKERS",
                "빈티지샵에서 구매한 상품"
        ));

        Page<SearchProduct> result = searchRepository.searchByCategory(
                "sneakers",
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategory())
                .isEqualTo("SNEAKERS");
    }

    @Test
    void searchesByStoryContentContainingKeyword() {
        searchRepository.save(createProduct(
                1L,
                "나이키 에어포스",
                "SNEAKERS",
                "빈티지샵에서 구매한 상품"
        ));

        Page<SearchProduct> result = searchRepository.searchByStoryContent(
                "빈티지샵",
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStoryContent())
                .contains("빈티지샵");
    }

    private SearchProduct createProduct(
            final Long productId,
            final String productName,
            final String category,
            final String storyContent
    ) {
        return new SearchProduct(
                productId,
                productName,
                "CW2288-111",
                category,
                LocalDate.of(2025, 1, 1),
                new BigDecimal("139000"),
                "IMMEDIATE",
                100L,
                "화이트 운동화",
                storyContent,
                LocalDateTime.of(2025, 1, 1, 12, 0)
        );
    }
}