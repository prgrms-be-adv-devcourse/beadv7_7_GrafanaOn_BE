package shop.dear.commerce.order.purchase.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.order.purchase.application.dto.CreatePurchaseCommand;
import shop.dear.commerce.order.purchase.application.port.ProductPort;
import shop.dear.commerce.order.purchase.application.port.dto.ProductInfo;
import shop.dear.commerce.order.purchase.application.port.dto.ProductSaleType;
import shop.dear.commerce.order.purchase.application.port.dto.ProductStatus;
import shop.dear.commerce.order.purchase.domain.exception.PurchaseErrorCode;
import shop.dear.commerce.order.purchase.domain.model.Purchase;
import shop.dear.commerce.order.purchase.infrastructure.outbox.CompensationOutboxWriter;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PurchaseFacadeTest {

    @Mock
    private PurchaseService purchaseService;

    @Mock
    private ProductPort productPort;

    @Mock
    private CompensationOutboxWriter compensationOutboxWriter;

    private PurchaseFacade purchaseFacade;

    @BeforeEach
    void setUp() {
        purchaseFacade = new PurchaseFacade(purchaseService, productPort, compensationOutboxWriter);
    }

    @Nested
    @DisplayName("createPurchase")
    class CreatePurchase {

        @Test
        @DisplayName("정상 흐름이면 보상 기록 없이 구매를 반환한다")
        void createsPurchase_withoutCompensation() {
            // given
            final CreatePurchaseCommand command = createCommand();
            final ProductInfo product = productInfo();
            final Purchase purchase = Purchase.create(1L, 2L, 10L, new BigDecimal("10000"), "서울시 강남구", LocalDateTime.now());

            given(purchaseService.validateAndGetProduct(1L, 10L)).willReturn(product);
            given(productPort.tradeProduct(10L)).willReturn(true);
            given(purchaseService.createPurchase(command, product)).willReturn(purchase);

            // when
            final Purchase result = purchaseFacade.createPurchase(command);

            // then
            assertThat(result).isEqualTo(purchase);
            verify(compensationOutboxWriter, never()).recordTradeRevertFailure(any());
        }

        @Test
        @DisplayName("이미 거래 중인 상품이면 보상 없이 예외를 던진다")
        void throwsException_whenProductAlreadyTrading() {
            // given
            final CreatePurchaseCommand command = createCommand();
            final ProductInfo product = productInfo();

            given(purchaseService.validateAndGetProduct(1L, 10L)).willReturn(product);
            given(productPort.tradeProduct(10L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> purchaseFacade.createPurchase(command))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PurchaseErrorCode.PRODUCT_ALREADY_TRADING);

            verify(purchaseService, never()).createPurchase(any(), any());
            verify(compensationOutboxWriter, never()).recordTradeRevertFailure(any());
        }

        @Test
        @DisplayName("사전 검증 실패 시 상품 상태를 변경하지 않고 보상도 기록하지 않는다")
        void doesNotCompensate_whenPreValidationFails() {
            // given
            final CreatePurchaseCommand command = createCommand();

            willThrow(new BusinessException(PurchaseErrorCode.PRODUCT_NOT_ON_SALE))
                    .given(purchaseService)
                    .validateAndGetProduct(1L, 10L);

            // when & then
            assertThatThrownBy(() -> purchaseFacade.createPurchase(command))
                    .isInstanceOf(BusinessException.class);

            verify(productPort, never()).tradeProduct(any());
            verify(compensationOutboxWriter, never()).recordTradeRevertFailure(any());
        }

        @Test
        @DisplayName("상품 상태 변경 이후 구매 생성이 실패하면 보상 outbox를 기록하고 원래 예외를 던진다")
        void recordsCompensation_whenCreatePurchaseFailsAfterTrade() {
            // given
            final CreatePurchaseCommand command = createCommand();
            final ProductInfo product = productInfo();

            given(purchaseService.validateAndGetProduct(1L, 10L)).willReturn(product);
            given(productPort.tradeProduct(10L)).willReturn(true);
            willThrow(new IllegalStateException("DB 저장 실패"))
                    .given(purchaseService)
                    .createPurchase(command, product);

            // when & then
            assertThatThrownBy(() -> purchaseFacade.createPurchase(command))
                    .isInstanceOf(IllegalStateException.class);

            verify(compensationOutboxWriter).recordTradeRevertFailure(10L);
        }

        private ProductInfo productInfo() {
            return new ProductInfo(
                    2L,
                    List.of(),
                    "상품명",
                    "브랜드",
                    new BigDecimal("10000"),
                    "MODEL-001",
                    "카테고리",
                    LocalDate.of(2026, 1, 1),
                    ProductSaleType.IMMEDIATE,
                    ProductStatus.ON_SALE,
                    0L,
                    "상품 설명",
                    LocalDateTime.now()
            );
        }

        private CreatePurchaseCommand createCommand() {
            return new CreatePurchaseCommand(1L, 10L, "서울시 강남구");
        }
    }
}
