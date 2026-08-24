package shop.dear.commerce.search.infrastructure;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.data.domain.PageRequest;
import shop.dear.commerce.search.domain.SearchProduct;
import shop.dear.commerce.search.infrastructure.elasticsearch.EsSearchAdapter;
import shop.dear.common.exception.ServiceUnavailableException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * 이중 쓰기 어댑터의 계약을 검증한다.
 *
 * 저장·삭제 책임은 두 어댑터에 위임하는 것과, Elasticsearch 실패를 삼켜
 * 원본 기록을 지키는 것이다. 조회 책임은 ES가 반복 실패하면 차단기가 열려
 * JPA로 전환되는 것과, 그 전환 경로 자체가 부하를 키우지 않는 것이다.
 *
 * CircuitBreakerFactory와 CircuitBreaker를 목으로 두고 run(supplier, fallback)의
 * 두 인자를 직접 호출해 검증한다. 실제 Resilience4j 슬라이딩 윈도 상태에 기대지 않기
 * 위해서다. Bulkhead는 Resilience4j 실제 객체를 쓴다. 가볍고, 상태를 직접 만들 수 있다.
 */
@ExtendWith(MockitoExtension.class)
class DualWriteSearchAdapterTest {

    private static final Long PRODUCT_ID = 999999L;

    @Mock
    private SearchRepositoryAdapter jpaAdapter;

    @Mock
    private EsSearchAdapter esAdapter;

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    private Bulkhead jpaSearchFallbackBulkhead;

    private DualWriteSearchAdapter dualWriteSearchAdapter;

    @BeforeEach
    void setUp() {
        jpaSearchFallbackBulkhead = Bulkhead.of("test-jpa-fallback", BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .maxWaitDuration(Duration.ZERO)
                .build());

        dualWriteSearchAdapter = new DualWriteSearchAdapter(
                jpaAdapter,
                esAdapter,
                circuitBreakerFactory,
                jpaSearchFallbackBulkhead
        );
    }

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
     * 차단기가 CLOSED 상태(정상)면 실제 호출인 첫 번째 인자를 실행한다.
     * ES가 응답하면 JPA로 넘어가지 않는다.
     */
    @Test
    void searchesElasticsearchWhenCircuitBreakerIsClosed() {
        givenCircuitBreakerRunsPrimarySupplier();
        given(esAdapter.searchByProductName(any(), any())).willReturn(null);

        dualWriteSearchAdapter.searchByProductName("에어포스", PageRequest.of(0, 20));

        verify(esAdapter).searchByProductName("에어포스", PageRequest.of(0, 20));
    }

    /**
     * 차단기가 fallback을 부르면(ES 실패 또는 OPEN 상태) JPA가 대신 처리한다.
     */
    @Test
    void fallsBackToJpaWhenCircuitBreakerInvokesFallback() {
        givenCircuitBreakerRunsFallback(new RuntimeException("ES 연결 실패"));
        given(jpaAdapter.searchByProductName(any(), any())).willReturn(null);

        dualWriteSearchAdapter.searchByProductName("에어포스", PageRequest.of(0, 20));

        verify(jpaAdapter).searchByProductName("에어포스", PageRequest.of(0, 20));
    }

    /**
     * JPA 폴백 동시 실행 한도를 넘으면 대기하지 않고 즉시 503으로 실패한다.
     * 대기를 허용하면 폴백 자체가 스레드를 붙잡아 부하를 키운다.
     */
    @Test
    void rejectsImmediatelyWhenJpaFallbackIsSaturated() {
        givenCircuitBreakerRunsFallback(new RuntimeException("ES 연결 실패"));

        // 한도(1)를 미리 채워, 다음 폴백 호출이 자리를 못 잡게 만든다.
        jpaSearchFallbackBulkhead.tryAcquirePermission();

        assertThatThrownBy(() ->
                dualWriteSearchAdapter.searchByProductName("에어포스", PageRequest.of(0, 20))
        ).isInstanceOf(ServiceUnavailableException.class);
    }

    @SuppressWarnings("unchecked")
    private void givenCircuitBreakerRunsPrimarySupplier() {
        given(circuitBreakerFactory.create(any())).willReturn(circuitBreaker);
        given(circuitBreaker.run(any(), any())).willAnswer(invocation -> {
            Supplier<Object> primary = invocation.getArgument(0);
            return primary.get();
        });
    }

    @SuppressWarnings("unchecked")
    private void givenCircuitBreakerRunsFallback(final Throwable cause) {
        given(circuitBreakerFactory.create(any())).willReturn(circuitBreaker);
        given(circuitBreaker.run(any(), any())).willAnswer(invocation -> {
            Function<Throwable, Object> fallback = invocation.getArgument(1);
            return fallback.apply(cause);
        });
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