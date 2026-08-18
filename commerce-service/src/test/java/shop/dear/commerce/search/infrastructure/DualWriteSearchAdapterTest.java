package shop.dear.commerce.search.infrastructure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import shop.dear.commerce.search.domain.SearchProduct;
import shop.dear.commerce.search.domain.SearchRepository;
import shop.dear.commerce.search.infrastructure.elasticsearch.SearchProductDocument;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이중 쓰기 어댑터가 양쪽 저장소에 기록하는지 확인한다.
 */
@SpringBootTest
class DualWriteSearchAdapterTest {

    private static final Long TEST_PRODUCT_ID = 999999L;

    @Autowired
    private SearchRepository searchRepository;

    @Autowired
    private SearchJpaRepository searchJpaRepository;

    @Autowired
    private ElasticsearchOperations operations;

    // ES 는 ddl-auto 의 영향을 받지 않아 문서가 남으므로 직접 지운다.
    // ES 장애 상황을 재현하는 테스트도 있어, 여기서 실패해도 테스트를 깨뜨리지 않는다.
    @AfterEach
    void cleanUpIndex() {
        try {
            operations.delete(String.valueOf(TEST_PRODUCT_ID), SearchProductDocument.class);
        } catch (Exception e) {
            // ES 가 내려가 있는 경우
        }
    }

    @Test
    void injectsDualWriteAdapter() {
        assertThat(searchRepository).isInstanceOf(DualWriteSearchAdapter.class);
    }

    @Test
    void savesToBothStores() {
        searchRepository.save(createProduct());

        // 색인 직후에는 검색에 잡히지 않는다. 기본 refresh 주기가 1초이므로 강제로 갱신한다.
        operations.indexOps(SearchProductDocument.class).refresh();

        assertThat(searchJpaRepository.findById(TEST_PRODUCT_ID)).isPresent();
        assertThat(operations.get(String.valueOf(TEST_PRODUCT_ID), SearchProductDocument.class))
                .isNotNull();
    }

    /**
     * ES 장애 시에도 원본 테이블 기록은 남아야 한다.
     * 남지 않으면 재색인으로 복구할 수 없다.
     *
     * ES 를 내려야 재현되므로 평소에는 실행하지 않는다.
     *   docker stop elasticsearch-local
     *   ./gradlew :commerce-service:test --tests '*DualWriteSearchAdapterTest*'
     *   docker start elasticsearch-local
     */
    @Disabled("ES 를 내린 상태에서 수동으로 실행한다")
    @Test
    void keepsJpaRecordWhenElasticsearchFails() {
        searchRepository.save(createProduct());

        assertThat(searchJpaRepository.findById(TEST_PRODUCT_ID)).isPresent();
    }

    private SearchProduct createProduct() {
        return new SearchProduct(
                TEST_PRODUCT_ID,
                "이중쓰기 테스트 상품",
                "DUAL-001",
                "SNEAKERS",
                LocalDate.of(2025, 1, 1),
                new BigDecimal("139000"),
                "IMMEDIATE",
                0L,
                "확인용",
                "이중쓰기 확인용 스토리",
                LocalDateTime.now()
        );
    }
}