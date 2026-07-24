package shop.dear.commerce.order.offer.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.order.offer.application.port.OfferEventPublisher;
import shop.dear.commerce.order.offer.domain.constant.OfferStatus;
import shop.dear.commerce.order.offer.domain.model.Offer;
import shop.dear.commerce.order.offer.domain.repository.OfferRepository;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

  @Mock
  private OfferRepository offerRepository;

  @Mock
  private OfferEventPublisher offerEventPublisher;

  @InjectMocks
  private OfferService offerService;

  @Nested
  @DisplayName("existsActiveOfferByProductId")
  class ExistsActiveOfferByProductId {

    @Test
    @DisplayName("PENDING 또는 ACCEPTED 상태의 오퍼가 존재하면 true를 반환한다")
    void returnsTrue_whenActiveOfferExists() {
      // given
      final Long productId = 1L;
      when(offerRepository.existsByProductIdAndStatusIn(eq(productId), anyList()))
              .thenReturn(true);

      // when
      final boolean result = offerService.existsActiveOfferByProductId(productId);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("PENDING, ACCEPTED 상태의 오퍼가 없으면 false를 반환한다")
    void returnsFalse_whenNoActiveOfferExists() {
      // given
      final Long productId = 1L;
      when(offerRepository.existsByProductIdAndStatusIn(eq(productId), anyList()))
              .thenReturn(false);

      // when
      final boolean result = offerService.existsActiveOfferByProductId(productId);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("REJECTED, CANCELLED 상태만 필터링하고 PENDING, ACCEPTED만 조회 조건으로 넘긴다")
    void queriesOnlyPendingAndAcceptedStatuses() {
      // given
      final Long productId = 1L;
      when(offerRepository.existsByProductIdAndStatusIn(eq(productId), anyList()))
              .thenReturn(true);

      // when
      offerService.existsActiveOfferByProductId(productId);

      // then
      verify(offerRepository).existsByProductIdAndStatusIn(
              eq(productId),
              eq(List.of(OfferStatus.PENDING, OfferStatus.ACCEPTED))
      );
    }
  }

  @Nested
  @DisplayName("acceptOffer")
  class AcceptOffer {

    @Test
    @DisplayName("오퍼를 수락하면 상태가 ACCEPTED로 변경되고 OfferAcceptedEvent를 발행한다")
    void acceptsOfferAndPublishesEvent() {
      // given
      final Offer offer = createPendingPaidOffer();
      when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

      // when
      offerService.acceptOffer(1L);

      // then
      assertThat(offer.getStatus()).isEqualTo(OfferStatus.ACCEPTED);

      final ArgumentCaptor<FinishedOrderEvent> captor = ArgumentCaptor.forClass(FinishedOrderEvent.class);
      verify(offerEventPublisher).publish(captor.capture());

      final FinishedOrderEvent event = captor.getValue();
      assertThat(event.productId()).isEqualTo(offer.getProductId());
      assertThat(event.sellerId()).isEqualTo(offer.getSellerId());
    }

    @Test
    @DisplayName("존재하지 않는 오퍼면 예외를 던진다")
    void throwsException_whenOfferNotFound() {
      // given
      when(offerRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> offerService.acceptOffer(1L))
              .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("PENDING 상태가 아니면 예외를 던지고 이벤트를 발행하지 않는다")
    void throwsException_whenStatusIsNotPending() {
      // given
      final Offer offer = createPendingPaidOffer();
      offer.reject(); // PENDING -> REJECTED로 전이시켜 조건 불충족 상태로 만듦
      when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

      // when & then
      assertThatThrownBy(() -> offerService.acceptOffer(1L))
              .isInstanceOf(BusinessException.class);
    }

    private Offer createPendingPaidOffer() {
      final Offer offer = Offer.create(
              1L, 2L, 3L, BigDecimal.valueOf(10000), "title", "story", "delivery"
      );
      offer.markPaid();
      return offer;
    }
  }
}
