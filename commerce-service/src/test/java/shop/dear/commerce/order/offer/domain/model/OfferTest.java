package shop.dear.commerce.order.offer.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shop.dear.commerce.order.offer.domain.constant.OfferStatus;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfferTest {

    @Test
    @DisplayName("PENDING 상태의 오퍼는 PRODUCT_DELETED로 전환할 수 있다")
    void markProductDeleted_fromPending() {
        // given
        final Offer offer = Offer.create(
                1L,
                2L,
                3L,
                10L,
                BigDecimal.valueOf(10000),
                "title",
                "story",
                "delivery"
        );

        // when
        offer.markProductDeleted();

        // then
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.PRODUCT_DELETED);
    }

    @Test
    @DisplayName("ACCEPTED 상태의 오퍼는 PRODUCT_DELETED로 전환할 수 있다")
    void markProductDeleted_fromAccepted() {
        // given
        final Offer offer = Offer.create(
                1L,
                2L,
                3L,
                10L,
                BigDecimal.valueOf(10000),
                "title",
                "story",
                "delivery"
        );
        offer.markPaid();
        offer.accept();

        // when
        offer.markProductDeleted();

        // then
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.PRODUCT_DELETED);
    }

    @Test
    @DisplayName("REJECTED 상태의 오퍼는 PRODUCT_DELETED로 전환할 수 없다")
    void markProductDeleted_fails_whenRejected() {
        // given
        final Offer offer = createPendingOffer();
        offer.reject();

        // when & then
        assertThatThrownBy(offer::markProductDeleted)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("CANCELLED 상태의 오퍼는 PRODUCT_DELETED로 전환할 수 없다")
    void markProductDeleted_fails_whenCancelled() {
        // given
        final Offer offer = createPendingOffer();
        offer.cancel();

        // when & then
        assertThatThrownBy(offer::markProductDeleted)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("PRODUCT_DELETED 상태의 오퍼는 PRODUCT_DELETED로 전환할 수 없다")
    void markProductDeleted_fails_whenAlreadyProductDeleted() {
        // given
        final Offer offer = createPendingOffer();
        offer.markProductDeleted();

        // when & then
        assertThatThrownBy(offer::markProductDeleted)
                .isInstanceOf(BusinessException.class);
    }

    private Offer createPendingOffer() {
        return Offer.create(
                1L,
                2L,
                3L,
                10L,
                BigDecimal.valueOf(10000),
                "title",
                "story",
                "delivery"
        );
    }
}
