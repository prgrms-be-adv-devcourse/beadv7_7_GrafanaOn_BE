package shop.dear.commerce.order.offer.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.order.offer.domain.constant.OfferStatus;
import shop.dear.commerce.order.offer.domain.repository.OfferRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

  @Mock
  private OfferRepository offerRepository;

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
}
