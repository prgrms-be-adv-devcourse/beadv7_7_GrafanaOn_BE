package shop.dear.commerce.search.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import shop.dear.commerce.search.domain.SearchProduct;
import shop.dear.commerce.search.infrastructure.elasticsearch.EsSearchAdapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * 이중 쓰기 어댑터의 계약을 검증한다.
 *
 * 실제 저장소는 필요하지 않다. 이 클래스의 책임은 두 어댑터에 위임하는 것과,
 * Elasticsearch 실패를 삼켜 원본 기록을 지키는 것뿐이다.
 */
@ExtendWith(MockitoExtension.class)
class DualWriteSearchAdapterTest {

    private static final Long PRODUCT_ID = 999999L;

    @Mock
    private SearchRepositoryAdapter jpaAdapter;

    @Mock
    private EsSearchAdapter esAdapter;

    @InjectMocks
    private DualWriteSearchAdapter dualWriteSearchAdapter;

    @Test
    void savesToBothAdapters() {
        final SearchProduct product = createProduct();

        dualWriteSearchAdapter.save(product);

        verify(jpaAdapter).save(product);
        verify(esAdapter).save(product);
    }

    /**
     * search_product 테이블은 재색인의 원본이다.
     * Elasticsearch 쓰기가 실패했다고 예외를 밖으로 던지면 리스너의 트랜잭션이 롤백되어
     * 테이블 기록까지 사라지고, 그러면 재색인으로도 복구할 수 없게 된다.
     */
    @Test
    void keepsJpaRecordWhenElasticsearchFails() {
        final SearchProduct product = createProduct();
        doThrow(new RuntimeException("ES 연결 실패")).when(esAdapter).save(product);

        assertThatCode(() -> dualWriteSearchAdapter.save(product)).doesNotThrowAnyException();

        verify(jpaAdapter).save(product);
    }

    @Test
    void deletesFromBothAdapters() {
        dualWriteSearchAdapter.deleteByProductId(PRODUCT_ID);

        verify(jpaAdapter).deleteByProductId(PRODUCT_ID);
        verify(esAdapter).deleteByProductId(PRODUCT_ID);
    }

    @Test
    void keepsJpaDeletionWhenElasticsearchFails() {
        doThrow(new RuntimeException("ES 연결 실패")).when(esAdapter).deleteByProductId(PRODUCT_ID);

        assertThatCode(() -> dualWriteSearchAdapter.deleteByProductId(PRODUCT_ID))
                .doesNotThrowAnyException();

        verify(jpaAdapter).deleteByProductId(PRODUCT_ID);
    }

    /**
     * 조회는 Elasticsearch 로만 간다. 테이블은 재색인 원본일 뿐 조회에 쓰이지 않는다.
     */
    @Test
    void delegatesSearchToElasticsearch() {
        given(esAdapter.searchByProductName(any(), any())).willReturn(null);

        dualWriteSearchAdapter.searchByProductName("에어포스", PageRequest.of(0, 20));

        verify(esAdapter).searchByProductName("에어포스", PageRequest.of(0, 20));
    }

    private SearchProduct createProduct() {
        return new SearchProduct(
                PRODUCT_ID,
                "이중쓰기 테스트 상품",
                "DUAL-001",
                "SEARCH_TEST_CATEGORY",
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