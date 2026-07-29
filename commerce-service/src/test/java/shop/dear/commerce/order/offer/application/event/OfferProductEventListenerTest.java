package shop.dear.commerce.order.offer.application.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.common.event.product.ProductDeletedEvent;
import shop.dear.commerce.order.offer.domain.constant.OfferStatus;
import shop.dear.commerce.order.offer.domain.model.Offer;
import shop.dear.commerce.order.offer.domain.repository.OfferRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OfferProductEventListenerTest {

    @Mock
    private OfferRepository offerRepository;

    @InjectMocks
    private OfferProductEventListener offerProductEventListener;

    @Test
    @DisplayName("상품 삭제 이벤트 발생 시 해당 상품의 PENDING, ACCEPTED 오퍼 상태를 PRODUCT_DELETED로 변경한다")
    void productDeletedEvent_changesActiveOffersToProductDeleted() {
        // given
        final Long productId = 1L;
        final ProductDeletedEvent event = new ProductDeletedEvent(productId);

        final Offer pendingOffer = createOffer(productId, OfferStatus.PENDING);
        final Offer acceptedOffer = createOffer(productId, OfferStatus.ACCEPTED);

        given(offerRepository.findByProductIdAndStatusInOrderByInsertedAtDesc(eq(productId), anyList()))
                .willReturn(List.of(pendingOffer, acceptedOffer));

        // when
        offerProductEventListener.handleProductDeleted(event);

        // then
        ArgumentCaptor<List<OfferStatus>> statusCaptor = ArgumentCaptor.forClass(List.class);
        verify(offerRepository).findByProductIdAndStatusInOrderByInsertedAtDesc(eq(productId), statusCaptor.capture());
        assertThat(statusCaptor.getValue()).containsExactlyInAnyOrder(OfferStatus.PENDING, OfferStatus.ACCEPTED);

        assertThat(pendingOffer.getStatus()).isEqualTo(OfferStatus.PRODUCT_DELETED);
        assertThat(acceptedOffer.getStatus()).isEqualTo(OfferStatus.PRODUCT_DELETED);
    }

    @Test
    @DisplayName("활성 오퍼가 없으면 상태를 변경하지 않는다")
    void productDeletedEvent_doesNothing_whenNoActiveOffers() {
        // given
        final Long productId = 1L;
        final ProductDeletedEvent event = new ProductDeletedEvent(productId);

        given(offerRepository.findByProductIdAndStatusInOrderByInsertedAtDesc(eq(productId), anyList()))
                .willReturn(List.of());

        // when
        offerProductEventListener.handleProductDeleted(event);

        // then
        verify(offerRepository).findByProductIdAndStatusInOrderByInsertedAtDesc(eq(productId), anyList());
    }

    private Offer createOffer(final Long productId, final OfferStatus status) {
        final Offer offer = Offer.create(
                1L,
                2L,
                productId,
                10L,
                BigDecimal.valueOf(10000),
                "title",
                "story",
                "delivery"
        );

        if (status == OfferStatus.ACCEPTED) {
            offer.markPaid();
            offer.accept();
        }
        if (status == OfferStatus.REJECTED) {
            offer.reject();
        }
        if (status == OfferStatus.CANCELLED) {
            offer.cancel();
        }
        if (status == OfferStatus.PRODUCT_DELETED) {
            offer.markProductDeleted();
        }

        return offer;
    }
}
