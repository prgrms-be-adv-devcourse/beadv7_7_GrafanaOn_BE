package shop.dear.commerce.order.purchase.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.order.common.application.port.MemberPort;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ProductPort productPort;

    @Mock
    private PurchaseEventPublisher purchaseEventPublisher;

    @Mock
    private MemberPort memberPort;

    @InjectMocks
    private PurchaseService purchaseService;

    @Nested
    @DisplayName("createPurchase")
    class CreatePurchase {

        @Test
        @DisplayName("판매 중인 즉시구매 상품이면 상품 응답 정보로 구매를 생성한다")
        void createsPurchase_whenProductIsOnSaleAndImmediate() {
            // given
            final CreatePurchaseCommand command = createCommand(1L, 10L);
            final ProductInfo product = productInfo(10L, 2L, ProductSaleType.IMMEDIATE, ProductStatus.ON_SALE);
            stubMemberExists(1L);
            given(productPort.getProduct(10L)).willReturn(product);
            given(purchaseRepository.save(any(Purchase.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            final Purchase purchase = purchaseService.createPurchase(command);

            // then
            assertThat(purchase.getBuyerId()).isEqualTo(1L);
            assertThat(purchase.getSellerId()).isEqualTo(2L);
            assertThat(purchase.getProductId()).isEqualTo(10L);
            assertThat(purchase.getAmount()).isEqualTo(new BigDecimal("10000"));
            verifyMemberExists(1L);
            verify(purchaseRepository).save(any(Purchase.class));
        }

        @Test
        @DisplayName("존재하지 않는 구매자면 예외를 던진다")
        void throwsException_whenBuyerNotExists() {
            // given
            final CreatePurchaseCommand command = createCommand(1L, 10L);
            willThrow(new BusinessException(PurchaseErrorCode.PURCHASE_NOT_FOUND))
                    .given(memberPort).validateMemberExists(1L);

            // when & then
            assertThatThrownBy(() -> purchaseService.createPurchase(command))
                    .isInstanceOf(BusinessException.class);

            verify(productPort, never()).getProduct(anyLong());
            verify(purchaseRepository, never()).save(any(Purchase.class));
        }

        @Test
        @DisplayName("판매 중이 아닌 상품은 구매할 수 없다")
        void rejectsProductNotOnSale() {
            assertCreatePurchaseError(
                    productInfo(10L, 2L, ProductSaleType.IMMEDIATE, ProductStatus.SOLD_OUT),
                    PurchaseErrorCode.PRODUCT_NOT_ON_SALE
            );
        }

        @Test
        @DisplayName("가격 제안 상품은 즉시구매할 수 없다")
        void rejectsOfferProduct() {
            assertCreatePurchaseError(
                    productInfo(10L, 2L, ProductSaleType.OFFER, ProductStatus.ON_SALE),
                    PurchaseErrorCode.PRODUCT_NOT_FOR_IMMEDIATE_PURCHASE
            );
        }

        @Test
        @DisplayName("판매자는 본인 상품을 구매할 수 없다")
        void rejectsOwnProduct() {
            assertCreatePurchaseError(
                    productInfo(10L, 1L, ProductSaleType.IMMEDIATE, ProductStatus.ON_SALE),
                    PurchaseErrorCode.CANNOT_PURCHASE_OWN_PRODUCT
            );
        }

        @Test
        @DisplayName("요청 상품과 응답 상품 식별자가 다르면 구매하지 않는다")
        void rejectsMismatchedProductResponse() {
            assertCreatePurchaseError(
                    productInfo(11L, 2L, ProductSaleType.IMMEDIATE, ProductStatus.ON_SALE),
                    PurchaseErrorCode.INVALID_PRODUCT_RESPONSE
            );
        }

        @Test
        @DisplayName("상품 응답이 없으면 구매하지 않는다")
        void rejectsEmptyProductResponse() {
            assertCreatePurchaseError(null, PurchaseErrorCode.INVALID_PRODUCT_RESPONSE);
        }

        private ProductInfo productInfo(
                final Long productId,
                final Long sellerId,
                final ProductSaleType saleType,
                final ProductStatus status
        ) {
            return new ProductInfo(productId, sellerId, new BigDecimal("10000"), saleType, status);
        }

        private CreatePurchaseCommand createCommand(final Long buyerId, final Long productId) {
            return new CreatePurchaseCommand(buyerId, productId, "서울시 강남구");
        }

        private void assertCreatePurchaseError(final ProductInfo product, final PurchaseErrorCode errorCode) {
            stubMemberExists(1L);
            given(productPort.getProduct(10L)).willReturn(product);

            assertThatThrownBy(() -> purchaseService.createPurchase(createCommand(1L, 10L)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(throwable -> assertThat(((BusinessException) throwable).getErrorCode()).isEqualTo(errorCode));

            verifyMemberExists(1L);
            verify(purchaseRepository, never()).save(any(Purchase.class));
        }
    }

    @Nested
    @DisplayName("confirmPurchase")
    class ConfirmPurchase {

        @Test
        @DisplayName("구매를 확정하면 상태가 PURCHASE_CONFIRMED로 변경되고 FinishedOrderEvent를 발행한다")
        void confirmsPurchaseAndPublishesEvent() {
            // given
            final Purchase purchase = createPaidPurchase();
            stubMemberExists(1L);
            given(purchaseRepository.findById(1L)).willReturn(Optional.of(purchase));

            // when
            purchaseService.confirmPurchase(1L, 1L);

            // then
            assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.PURCHASE_CONFIRMED);
            verifyMemberExists(1L);

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
            stubMemberExists(1L);
            given(purchaseRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> purchaseService.confirmPurchase(1L, 1L))
                    .isInstanceOf(BusinessException.class);

            verifyMemberExists(1L);
            verify(purchaseEventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("PAID 상태가 아니면 예외를 던지고 이벤트를 발행하지 않는다")
        void throwsException_whenStatusIsNotPaid() {
            // given
            final Purchase purchase = createPendingPaymentPurchase();
            stubMemberExists(1L);
            given(purchaseRepository.findById(1L)).willReturn(Optional.of(purchase));

            // when & then
            assertThatThrownBy(() -> purchaseService.confirmPurchase(1L, 1L))
                    .isInstanceOf(BusinessException.class);

            verifyMemberExists(1L);
            verify(purchaseEventPublisher, never()).publish(any());
        }
    }

    @Nested
    @DisplayName("cancelPurchase")
    class CancelPurchase {

        @Test
        @DisplayName("PENDING_PAYMENT 상태인 구매를 취소하면 상태가 CANCELLED로 변경된다")
        void cancelsPurchaseWhenPendingPayment() {
            // given
            final Purchase purchase = createPendingPaymentPurchase();
            stubMemberExists(1L);
            given(purchaseRepository.findById(1L)).willReturn(Optional.of(purchase));

            // when
            purchaseService.cancelPurchase(1L, 1L);

            // then
            assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.CANCELLED);
            verifyMemberExists(1L);
        }

        @Test
        @DisplayName("PAID 상태인 구매를 취소하면 상태가 CANCELLED로 변경된다")
        void cancelsPurchaseWhenPaid() {
            // given
            final Purchase purchase = createPaidPurchase();
            stubMemberExists(1L);
            given(purchaseRepository.findById(1L)).willReturn(Optional.of(purchase));

            // when
            purchaseService.cancelPurchase(1L, 1L);

            // then
            assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.CANCELLED);
            verifyMemberExists(1L);
        }

        @Test
        @DisplayName("존재하지 않는 구매를 취소하면 예외를 던진다")
        void throwsException_whenPurchaseNotFound() {
            // given
            stubMemberExists(1L);
            given(purchaseRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> purchaseService.cancelPurchase(1L, 1L))
                    .isInstanceOf(BusinessException.class);

            verifyMemberExists(1L);
        }

        @Test
        @DisplayName("다른 사용자의 구매를 취소하려 하면 예외를 던진다")
        void throwsException_whenBuyerMismatch() {
            // given
            final Purchase purchase = createPendingPaymentPurchase();
            stubMemberExists(999L);
            given(purchaseRepository.findById(1L)).willReturn(Optional.of(purchase));

            // when & then
            assertThatThrownBy(() -> purchaseService.cancelPurchase(1L, 999L))
                    .isInstanceOf(BusinessException.class);

            verifyMemberExists(999L);
        }
    }

    @Nested
    @DisplayName("getPurchase")
    class GetPurchase {

        @Test
        @DisplayName("구매 식별자로 구매를 조회하면 구매 정보를 반환한다")
        void returnsPurchase_whenPurchaseExists() {
            // given
            final Purchase purchase = createPendingPaymentPurchase();
            stubMemberExists(1L);
            given(purchaseRepository.findById(1L)).willReturn(Optional.of(purchase));

            // when
            final Purchase result = purchaseService.getPurchase(1L, 1L);

            // then
            assertThat(result).isEqualTo(purchase);
            verifyMemberExists(1L);
        }

        @Test
        @DisplayName("존재하지 않는 구매를 조회하면 예외를 던진다")
        void throwsException_whenPurchaseNotFound() {
            // given
            stubMemberExists(1L);
            given(purchaseRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> purchaseService.getPurchase(1L, 1L))
                    .isInstanceOf(BusinessException.class);

            verifyMemberExists(1L);
        }

        @Test
        @DisplayName("다른 사용자의 구매를 조회하면 예외를 던진다")
        void throwsException_whenBuyerMismatch() {
            // given
            final Purchase purchase = createPendingPaymentPurchase();
            stubMemberExists(999L);
            given(purchaseRepository.findById(1L)).willReturn(Optional.of(purchase));

            // when & then
            assertThatThrownBy(() -> purchaseService.getPurchase(1L, 999L))
                    .isInstanceOf(BusinessException.class);

            verifyMemberExists(999L);
        }
    }

    @Nested
    @DisplayName("getPurchasesByBuyerId")
    class GetPurchasesByBuyerId {

        @Test
        @DisplayName("구매자 식별자로 구매 목록을 조회하면 구매 목록을 반환한다")
        void returnsPurchases_whenBuyerHasPurchases() {
            // given
            final Purchase purchase1 = Purchase.create(
                    1L,
                    2L,
                    3L,
                    BigDecimal.valueOf(10000),
                    "delivery1",
                    null
            );
            final Purchase purchase2 = Purchase.create(
                    1L,
                    3L,
                    4L,
                    BigDecimal.valueOf(20000),
                    "delivery2",
                    null
            );
            stubMemberExists(1L);
            given(purchaseRepository.findByBuyerId(1L)).willReturn(List.of(purchase1, purchase2));

            // when
            final List<Purchase> result = purchaseService.getPurchasesByBuyerId(1L);

            // then
            assertThat(result).hasSize(2).containsExactly(purchase1, purchase2);
            verifyMemberExists(1L);
        }

        @Test
        @DisplayName("구매 기록이 없으면 빈 목록을 반환한다")
        void returnsEmptyList_whenBuyerHasNoPurchases() {
            // given
            stubMemberExists(1L);
            given(purchaseRepository.findByBuyerId(1L)).willReturn(List.of());

            // when
            final List<Purchase> result = purchaseService.getPurchasesByBuyerId(1L);

            // then
            assertThat(result).isEmpty();
            verifyMemberExists(1L);
        }
    }

    private void stubMemberExists(final Long memberId) {
        willDoNothing().given(memberPort).validateMemberExists(memberId);
    }

    private void verifyMemberExists(final Long memberId) {
        verify(memberPort).validateMemberExists(memberId);
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
