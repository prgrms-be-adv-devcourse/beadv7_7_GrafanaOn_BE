package shop.dear.commerce.order.purchase.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.order.purchase.application.port.PurchaseEventPublisher;
import shop.dear.commerce.order.purchase.domain.constant.PurchaseStatus;
import shop.dear.commerce.order.purchase.domain.model.Purchase;
import shop.dear.commerce.order.purchase.domain.repository.PurchaseRepository;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.event.order.OrderType;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

  @Mock
  private PurchaseRepository purchaseRepository;

  @Mock
  private PurchaseEventPublisher purchaseEventPublisher;

  @InjectMocks
  private PurchaseService purchaseService;

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

      verify(purchaseEventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
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

      verify(purchaseEventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
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
