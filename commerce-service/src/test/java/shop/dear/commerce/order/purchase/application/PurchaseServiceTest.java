package shop.dear.commerce.order.purchase.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.order.purchase.application.dto.CreatePurchaseCommand;
import shop.dear.commerce.order.purchase.application.port.ProductPort;
import shop.dear.commerce.order.purchase.application.port.PurchaseEventPublisher;
import shop.dear.commerce.order.purchase.application.port.dto.ProductInfo;
import shop.dear.commerce.order.purchase.application.port.dto.ProductSaleType;
import shop.dear.commerce.order.purchase.application.port.dto.ProductStatus;
import shop.dear.commerce.order.purchase.domain.constant.PurchaseStatus;
import shop.dear.commerce.order.purchase.domain.exception.PurchaseErrorCode;
import shop.dear.commerce.order.purchase.domain.model.Purchase;
import shop.dear.commerce.order.purchase.domain.repository.PurchaseRepository;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.event.order.OrderType;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ProductPort productPort;

    @Mock
    private PurchaseEventPublisher purchaseEventPublisher;

    @InjectMocks
    private PurchaseService purchaseService;

    private ProductInfo product(
        final Long productId,
        final Long sellerId,
        final ProductSaleType saleType,
        final ProductStatus status
    ) {
        return new ProductInfo(productId, sellerId, new BigDecimal("10000"), saleType, status);
    }

    private void assertPurchaseError(final ProductInfo product, final PurchaseErrorCode errorCode) {
        given(productPort.getProduct(10L)).willReturn(product);

        final BusinessException exception = assertThrows(
                BusinessException.class,
                () -> purchaseService.createPurchase(command(1L, 10L))
        );

        assertEquals(errorCode, exception.getErrorCode());
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    private CreatePurchaseCommand command(final Long buyerId, final Long productId) {
        return new CreatePurchaseCommand(buyerId, productId, "서울시 강남구");
    }

    @Test
    @DisplayName("판매 중인 즉시구매 상품이면 상품 응답 정보로 구매를 생성한다")
    void createPurchaseSuccess() {
        final CreatePurchaseCommand command = command(1L, 10L);
        final ProductInfo product = product(10L, 2L, ProductSaleType.IMMEDIATE, ProductStatus.ON_SALE);
        given(productPort.getProduct(10L)).willReturn(product);
        given(purchaseRepository.save(any(Purchase.class))).willAnswer(invocation -> invocation.getArgument(0));

        final Purchase purchase = purchaseService.createPurchase(command);

        assertEquals(1L, purchase.getBuyerId());
        assertEquals(2L, purchase.getSellerId());
        assertEquals(10L, purchase.getProductId());
        assertEquals(new BigDecimal("10000"), purchase.getAmount());
        verify(purchaseRepository).save(any(Purchase.class));
    }

    @Test
    @DisplayName("판매 중이 아닌 상품은 구매할 수 없다")
    void rejectProductNotOnSale() {
        assertPurchaseError(
            product(10L, 2L, ProductSaleType.IMMEDIATE, ProductStatus.SOLD_OUT),
            PurchaseErrorCode.PRODUCT_NOT_ON_SALE
        );
    }

    @Test
    @DisplayName("가격 제안 상품은 즉시구매할 수 없다")
    void rejectOfferProduct() {
        assertPurchaseError(
            product(10L, 2L, ProductSaleType.OFFER, ProductStatus.ON_SALE),
            PurchaseErrorCode.PRODUCT_NOT_FOR_IMMEDIATE_PURCHASE
        );
    }

    @Test
    @DisplayName("판매자는 본인 상품을 구매할 수 없다")
    void rejectOwnProduct() {
        assertPurchaseError(
            product(10L, 1L, ProductSaleType.IMMEDIATE, ProductStatus.ON_SALE),
            PurchaseErrorCode.CANNOT_PURCHASE_OWN_PRODUCT
        );
    }

    @Test
    @DisplayName("요청 상품과 응답 상품 식별자가 다르면 구매하지 않는다")
    void rejectMismatchedProductResponse() {
        assertPurchaseError(
            product(11L, 2L, ProductSaleType.IMMEDIATE, ProductStatus.ON_SALE),
            PurchaseErrorCode.INVALID_PRODUCT_RESPONSE
        );
    }

    @Test
    @DisplayName("상품 응답이 없으면 구매하지 않는다")
    void rejectEmptyProductResponse() {
        assertPurchaseError(null, PurchaseErrorCode.INVALID_PRODUCT_RESPONSE);
    }

    @Nested
    @DisplayName("confirmPurchase")
    class ConfirmPurchase {

        @Test
        @DisplayName("구매를 확정하면 상태가 PURCHASE_CONFIRMED로 변경되고 FinishedOrderEvent를 발행한다")
        void confirmsPurchaseAndPublishesEvent() {
            // given
            final Purchase purchase = createPaidPurchase();
            when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));

            // when
            purchaseService.confirmPurchase(1L);

            // then
            assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.PURCHASE_CONFIRMED);

            final ArgumentCaptor<FinishedOrderEvent> captor = ArgumentCaptor.forClass(FinishedOrderEvent.class);
            verify(purchaseEventPublisher).publish(captor.capture());

            final FinishedOrderEvent event = captor.getValue();
            assertThat(event.orderId()).isEqualTo(purchase.getId());
            assertThat(event.buyerId()).isEqualTo(purchase.getBuyerId());
            assertThat(event.sellerId()).isEqualTo(purchase.getSellerId());
            assertThat(event.productId()).isEqualTo(purchase.getProductId());
            assertThat(event.amount()).isEqualTo(purchase.getAmount());
            assertThat(event.orderType()).isEqualTo(OrderType.PURCHASE);
        }

        @Test
        @DisplayName("존재하지 않는 구매면 예외를 던진다")
        void throwsException_whenPurchaseNotFound() {
            // given
            when(purchaseRepository.findById(1L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> purchaseService.confirmPurchase(1L))
                    .isInstanceOf(BusinessException.class);

            verify(purchaseEventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("PAID 상태가 아니면 예외를 던지고 이벤트를 발행하지 않는다")
        void throwsException_whenStatusIsNotPaid() {
            // given
            final Purchase purchase = createPendingPaymentPurchase();
            when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));

            // when & then
            assertThatThrownBy(() -> purchaseService.confirmPurchase(1L))
                    .isInstanceOf(BusinessException.class);

            verify(purchaseEventPublisher, never()).publish(any());
        }

        private Purchase createPaidPurchase() {
            final Purchase purchase = Purchase.create(
                    1L,
                    2L,
                    3L,
                    BigDecimal.valueOf(10000),
                    "delivery",
                    null
            );
            purchase.pay();
            return purchase;
        }

        private Purchase createPendingPaymentPurchase() {
            return Purchase.create(
                    1L,
                    2L,
                    3L,
                    BigDecimal.valueOf(10000),
                    "delivery",
                    null
            );
        }
    }
}
