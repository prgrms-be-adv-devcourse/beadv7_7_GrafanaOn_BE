package shop.dear.commerce.search.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.search.application.event.ProductEventListener;
import shop.dear.common.event.product.ProductChangedEvent;
import shop.dear.common.event.product.ProductDeletedEvent;
import shop.dear.commerce.search.domain.SearchProduct;
import shop.dear.commerce.search.domain.SearchRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductEventListenerTest {

    @Mock
    private SearchRepository searchRepository;

    @InjectMocks
    private ProductEventListener productEventListener;

    @Test
    void preparingProductIsSaved() {
        ProductChangedEvent event = createChangedEvent("PREPARING");

        productEventListener.handleProductChanged(event);

        ArgumentCaptor<SearchProduct> captor =
                ArgumentCaptor.forClass(SearchProduct.class);

        verify(searchRepository).save(captor.capture());

        SearchProduct savedProduct = captor.getValue();
        assertThat(savedProduct.getProductId()).isEqualTo(1L);
        assertThat(savedProduct.getProductName()).isEqualTo("나이키 에어포스");
        assertThat(savedProduct.getStoryContent()).isEqualTo("빈티지샵에서 구매한 상품");
    }

    @Test
    void soldOutProductIsDeleted() {
        ProductChangedEvent event = createChangedEvent("SOLD_OUT");

        productEventListener.handleProductChanged(event);

        verify(searchRepository).deleteByProductId(1L);
        verify(searchRepository, never()).save(
                org.mockito.ArgumentMatchers.any(SearchProduct.class)
        );
    }

    @Test
    void deletedProductIsDeleted() {
        ProductDeletedEvent event = new ProductDeletedEvent(1L);

        productEventListener.handleProductDeleted(event);

        verify(searchRepository).deleteByProductId(1L);
    }

    private ProductChangedEvent createChangedEvent(final String status) {
        return new ProductChangedEvent(
                1L,
                "나이키 에어포스",
                "CW2288-111",
                "SNEAKERS",
                LocalDate.of(2025, 1, 1),
                new BigDecimal("139000"),
                "IMMEDIATE",
                status,
                100L,
                "화이트 운동화",
                "빈티지샵에서 구매한 상품",
                LocalDateTime.of(2025, 1, 1, 12, 0)
        );
    }
}