package shop.dear.commerce.order.offer.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.order.offer.application.dto.CreateOfferCommand;
import shop.dear.commerce.order.offer.application.dto.CreateOfferSnapshotCommand;
import shop.dear.commerce.order.offer.application.port.OfferEventPublisher;
import shop.dear.commerce.order.offer.domain.constant.OfferStatus;
import shop.dear.commerce.order.offer.domain.exception.OfferErrorCode;
import shop.dear.commerce.order.offer.domain.model.Offer;
import shop.dear.commerce.order.offer.domain.repository.OfferRepository;
import shop.dear.commerce.order.offersnapshot.domain.exception.OfferSnapshotErrorCode;
import shop.dear.commerce.order.offersnapshot.domain.model.OfferSnapshot;
import shop.dear.commerce.order.offersnapshot.domain.repository.OfferSnapshotRepository;
import shop.dear.commerce.order.offer.application.port.MemberPort;
import shop.dear.commerce.order.offer.application.port.ProductPort;
import shop.dear.commerce.order.offer.application.port.dto.ProductInfo;
import shop.dear.common.event.financial.PaymentHoldRequestedEvent;
import shop.dear.common.event.financial.PaymentRequestedEvent;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private OfferEventPublisher offerEventPublisher;

    @Mock
    private OfferSnapshotRepository offerSnapshotRepository;

    @Mock
    private ProductPort productPort;

    @Mock
    private MemberPort memberPort;

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
            given(offerRepository.existsByProductIdAndStatusIn(eq(productId), anyList()))
                    .willReturn(true);

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
            given(offerRepository.existsByProductIdAndStatusIn(eq(productId), anyList()))
                    .willReturn(false);

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
            given(offerRepository.existsByProductIdAndStatusIn(eq(productId), anyList()))
                    .willReturn(true);

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
        @DisplayName("오퍼를 수락하면 상태가 ACCEPTED로 변경되고 FinishedOrderEvent를 발행한다")
        void acceptsOfferAndPublishesEvent() {
            // given
            final Offer offer = createPendingPaidOffer();
            given(offerRepository.findById(1L)).willReturn(Optional.of(offer));

            // when
            offerService.acceptOffer(1L, 2L);

            // then
            assertThat(offer.getStatus()).isEqualTo(OfferStatus.ACCEPTED);

            final ArgumentCaptor<FinishedOrderEvent> captor =
                    ArgumentCaptor.forClass(FinishedOrderEvent.class);
            verify(offerEventPublisher).publish(captor.capture());

            final FinishedOrderEvent event = captor.getValue();
            assertThat(event.orderId()).isEqualTo(offer.getId());
            assertThat(event.buyerId()).isEqualTo(offer.getBuyerId());
            assertThat(event.sellerId()).isEqualTo(offer.getSellerId());
            assertThat(event.productId()).isEqualTo(offer.getProductId());
            assertThat(event.amount()).isEqualTo(offer.getAmount());

            final ArgumentCaptor<PaymentRequestedEvent> paymentCaptor =
                    ArgumentCaptor.forClass(PaymentRequestedEvent.class);
            verify(offerEventPublisher).publish(paymentCaptor.capture());

            final PaymentRequestedEvent paymentEvent = paymentCaptor.getValue();
            assertThat(paymentEvent.orderId()).isEqualTo(offer.getId());
            assertThat(paymentEvent.memberId()).isEqualTo(offer.getBuyerId());
            assertThat(paymentEvent.amount()).isEqualTo(offer.getAmount());
        }

        @Test
        @DisplayName("존재하지 않는 오퍼면 예외를 던진다")
        void throwsException_whenOfferNotFound() {
            // given
            given(offerRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> offerService.acceptOffer(1L, 2L))
                    .isInstanceOf(BusinessException.class);

            verifyNoInteractions(offerEventPublisher);
        }

        @Test
        @DisplayName("PENDING 상태가 아니면 예외를 던지고 이벤트를 발행하지 않는다")
        void throwsException_whenStatusIsNotPending() {
            // given
            final Offer offer = createPendingPaidOffer();
            offer.reject();
            given(offerRepository.findById(1L)).willReturn(Optional.of(offer));

            // when & then
            assertThatThrownBy(() -> offerService.acceptOffer(1L, 2L))
                    .isInstanceOf(BusinessException.class);

            verifyNoInteractions(offerEventPublisher);
        }

        private Offer createPendingPaidOffer() {
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
            return offer;
        }
    }

    @Nested
    @DisplayName("rejectOffer")
    class RejectOffer {

        @Test
        @DisplayName("오퍼를 거절하면 상태가 REJECTED로 변경된다")
        void rejectsOffer() {
            // given
            final Offer offer = createPendingPaidOffer();
            given(offerRepository.findById(1L)).willReturn(Optional.of(offer));
            given(offerRepository.save(offer)).willReturn(offer);

            // when
            offerService.rejectOffer(1L, 2L);

            // then
            assertThat(offer.getStatus()).isEqualTo(OfferStatus.REJECTED);
            verify(offerRepository).save(offer);
            verifyNoInteractions(offerEventPublisher);
        }

        @Test
        @DisplayName("존재하지 않는 오퍼면 예외를 던진다")
        void throwsException_whenOfferNotFound() {
            // given
            given(offerRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> offerService.rejectOffer(1L, 2L))
                    .isInstanceOf(BusinessException.class);

            verify(offerRepository, never()).save(any());
            verifyNoInteractions(offerEventPublisher);
        }

        @Test
        @DisplayName("판매자가 아니면 예외를 던진다")
        void throwsException_whenNotSeller() {
            // given
            final Offer offer = createPendingPaidOffer();
            given(offerRepository.findById(1L)).willReturn(Optional.of(offer));

            // when & then
            assertThatThrownBy(() -> offerService.rejectOffer(1L, 999L))
                    .isInstanceOf(BusinessException.class);

            verify(offerRepository, never()).save(any());
            verifyNoInteractions(offerEventPublisher);
        }

        @Test
        @DisplayName("PENDING 상태가 아니면 예외를 던진다")
        void throwsException_whenStatusIsNotPending() {
            // given
            final Offer offer = createPendingPaidOffer();
            offer.reject();
            given(offerRepository.findById(1L)).willReturn(Optional.of(offer));

            // when & then
            assertThatThrownBy(() -> offerService.rejectOffer(1L, 2L))
                    .isInstanceOf(BusinessException.class);

            verify(offerRepository, never()).save(any());
            verifyNoInteractions(offerEventPublisher);
        }

        private Offer createPendingPaidOffer() {
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
            return offer;
        }
    }

    @Nested
    @DisplayName("findOfferById")
    class FindOfferById {

        @Test
        @DisplayName("판매자가 본인 상품의 오퍼를 상세 조회한다")
        void returnsOffer_whenSeller() {
            // given
            final Offer offer = createPendingPaidOffer();
            given(offerRepository.findById(1L)).willReturn(Optional.of(offer));

            // when
            final Offer result = offerService.findOfferById(1L, 2L);

            // then
            assertThat(result.getId()).isEqualTo(offer.getId());
            assertThat(result.getSellerId()).isEqualTo(2L);
            verify(offerRepository).findById(1L);
        }

        @Test
        @DisplayName("구매자가 본인이 요청한 오퍼를 상세 조회한다")
        void returnsOffer_whenBuyer() {
            // given
            final Offer offer = createPendingPaidOffer();
            given(offerRepository.findById(1L)).willReturn(Optional.of(offer));

            // when
            final Offer result = offerService.findOfferById(1L, 1L);

            // then
            assertThat(result.getId()).isEqualTo(offer.getId());
            assertThat(result.getBuyerId()).isEqualTo(1L);
            verify(offerRepository).findById(1L);
        }

        @Test
        @DisplayName("오퍼가 존재하지 않으면 예외를 던진다")
        void throwsException_whenOfferNotFound() {
            // given
            given(offerRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> offerService.findOfferById(1L, 1L))
                    .isInstanceOf(BusinessException.class);

            verify(offerRepository).findById(1L);
        }

        @Test
        @DisplayName("판매자도 구매자도 아니면 예외를 던진다")
        void throwsException_whenNotAuthorized() {
            // given
            final Offer offer = createPendingPaidOffer();
            given(offerRepository.findById(1L)).willReturn(Optional.of(offer));

            // when & then
            assertThatThrownBy(() -> offerService.findOfferById(1L, 999L))
                    .isInstanceOf(BusinessException.class);

            verify(offerRepository).findById(1L);
        }

        private Offer createPendingPaidOffer() {
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
            return offer;
        }
    }

    @Nested
    @DisplayName("findOffersByProductId")
    class FindOffersByProductId {

        @Test
        @DisplayName("판매자가 등록한 특정 상품의 오퍼 목록을 조회한다")
        void returnsOffers_whenSellerOwnsProduct() {
            // given
            final Offer offer1 = createPendingPaidOffer();
            final Offer offer2 = createPendingPaidOffer();
            given(productPort.getProduct(10L)).willReturn(productInfo(2L));
            given(offerRepository.findByProductIdOrderByInsertedAtDesc(10L))
                    .willReturn(List.of(offer1, offer2));

            // when
            final List<Offer> offers = offerService.findOffersByProductId(2L, 10L, List.of());

            // then
            assertThat(offers).hasSize(2);
            assertThat(offers.get(0).getSellerId()).isEqualTo(2L);
            assertThat(offers.get(1).getSellerId()).isEqualTo(2L);
            verify(productPort).getProduct(10L);
            verify(offerRepository).findByProductIdOrderByInsertedAtDesc(10L);
        }

        @Test
        @DisplayName("상태 필터로 특정 상품의 오퍼 목록을 조회한다")
        void returnsOffersByStatus_whenSellerOwnsProduct() {
            // given
            final Offer offer1 = createPendingPaidOffer();
            final Offer offer2 = createPendingPaidOffer();
            offer2.reject();
            given(productPort.getProduct(10L)).willReturn(productInfo(2L));
            given(offerRepository.findByProductIdAndStatusInOrderByInsertedAtDesc(10L, List.of(OfferStatus.PENDING)))
                    .willReturn(List.of(offer1));

            // when
            final List<Offer> offers = offerService.findOffersByProductId(2L, 10L, List.of(OfferStatus.PENDING));

            // then
            assertThat(offers).hasSize(1);
            assertThat(offers.get(0).getStatus()).isEqualTo(OfferStatus.PENDING);
            verify(productPort).getProduct(10L);
            verify(offerRepository).findByProductIdAndStatusInOrderByInsertedAtDesc(10L, List.of(OfferStatus.PENDING));
        }

        @Test
        @DisplayName("접수된 오퍼가 없으면 빈 목록을 반환한다")
        void returnsEmptyList_whenNoOffers() {
            // given
            given(productPort.getProduct(10L)).willReturn(productInfo(2L));
            given(offerRepository.findByProductIdOrderByInsertedAtDesc(10L))
                    .willReturn(List.of());

            // when
            final List<Offer> offers = offerService.findOffersByProductId(2L, 10L, List.of());

            // then
            assertThat(offers).isEmpty();
            verify(productPort).getProduct(10L);
            verify(offerRepository).findByProductIdOrderByInsertedAtDesc(10L);
        }

        @Test
        @DisplayName("상품 판매자가 아니면 예외를 던진다")
        void throwsException_whenNotSeller() {
            // given
            given(productPort.getProduct(10L)).willReturn(productInfo(999L));

            // when & then
            assertThatThrownBy(() -> offerService.findOffersByProductId(2L, 10L, List.of()))
                    .isInstanceOf(BusinessException.class);

            verify(productPort).getProduct(10L);
            verify(offerRepository, never()).findByProductIdOrderByInsertedAtDesc(10L);
        }

        private Offer createPendingPaidOffer() {
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
            return offer;
        }
    }

    @Nested
    @DisplayName("createOfferSnapshot")
    class CreateOfferSnapshot {

        @Test
        @DisplayName("상품 정보를 스냅샷으로 저장한다")
        void createsSnapshot() {
            // given
            final CreateOfferSnapshotCommand command = new CreateOfferSnapshotCommand(1L, 10L);
            final ProductInfo product = productInfo(2L);

            stubMemberExists(1L);
            given(productPort.getProduct(10L)).willReturn(product);
            given(offerSnapshotRepository.findFirstByWriterIdAndProductId(1L, 10L))
                    .willReturn(Optional.empty());
            given(offerSnapshotRepository.save(any(OfferSnapshot.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            final OfferSnapshot snapshot = offerService.createOfferSnapshot(command);

            // then
            assertThat(snapshot.getWriterId()).isEqualTo(1L);
            assertThat(snapshot.getSellerId()).isEqualTo(2L);
            assertThat(snapshot.getProductId()).isEqualTo(10L);
            assertThat(snapshot.getModelNumberSnapshot()).isEqualTo("MODEL-001");
            assertThat(snapshot.getPriceSnapshot()).isEqualTo(new BigDecimal("10000"));

            verifyMemberExists(1L);
            verify(productPort).getProduct(10L);
            verify(offerSnapshotRepository).findFirstByWriterIdAndProductId(1L, 10L);
            verify(offerSnapshotRepository).save(any(OfferSnapshot.class));
        }

        @Test
        @DisplayName("동일한 작성자와 상품의 스냅샷이 이미 있으면 가격과 모델번호를 갱신한다")
        void updatesExistingSnapshot() {
            // given
            final CreateOfferSnapshotCommand command = new CreateOfferSnapshotCommand(1L, 10L);
            final ProductInfo product = productInfo(2L);
            final OfferSnapshot existing = OfferSnapshot.create(2L, 1L, 10L, "MODEL-OLD", new BigDecimal("5000"));

            stubMemberExists(1L);
            given(productPort.getProduct(10L)).willReturn(product);
            given(offerSnapshotRepository.findFirstByWriterIdAndProductId(1L, 10L))
                    .willReturn(Optional.of(existing));
            given(offerSnapshotRepository.save(existing))
                    .willReturn(existing);

            // when
            final OfferSnapshot snapshot = offerService.createOfferSnapshot(command);

            // then
            assertThat(snapshot.getModelNumberSnapshot()).isEqualTo("MODEL-001");
            assertThat(snapshot.getPriceSnapshot()).isEqualTo(new BigDecimal("10000"));

            verifyMemberExists(1L);
            verify(productPort).getProduct(10L);
            verify(offerSnapshotRepository).findFirstByWriterIdAndProductId(1L, 10L);
            verify(offerSnapshotRepository).save(existing);
        }
    }

    @Nested
    @DisplayName("createOffer")
    class CreateOffer {

        @Test
        @DisplayName("스냅샷 정보로 오퍼를 생성하고 가격이 일치하면 저장한다")
        void createsOffer_whenPriceMatches() {
            // given
            final OfferSnapshot snapshot = createSnapshot(1L, 2L, 10L, new BigDecimal("10000"));
            final CreateOfferCommand command = new CreateOfferCommand(1L, 1L, "title", "story", "delivery");

            stubMemberExists(1L);
            given(offerSnapshotRepository.findById(1L)).willReturn(Optional.of(snapshot));
            given(productPort.getProduct(10L)).willReturn(productInfo(2L));
            given(offerRepository.save(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            final Offer offer = offerService.createOffer(command);

            // then
            assertThat(offer.getBuyerId()).isEqualTo(1L);
            assertThat(offer.getSellerId()).isEqualTo(2L);
            assertThat(offer.getProductId()).isEqualTo(10L);
            assertThat(offer.getAmount()).isEqualTo(new BigDecimal("10000"));
            assertThat(snapshot.getOfferId()).isEqualTo(offer.getId());

            verifyMemberExists(1L);
            verify(offerSnapshotRepository).findById(1L);
            verify(productPort).getProduct(10L);
            verify(offerRepository).save(any());

            final ArgumentCaptor<PaymentHoldRequestedEvent> holdCaptor =
                    ArgumentCaptor.forClass(PaymentHoldRequestedEvent.class);
            verify(offerEventPublisher).publish(holdCaptor.capture());

            final PaymentHoldRequestedEvent holdEvent = holdCaptor.getValue();
            assertThat(holdEvent.orderId()).isEqualTo(offer.getId());
            assertThat(holdEvent.memberId()).isEqualTo(offer.getBuyerId());
            assertThat(holdEvent.amount()).isEqualTo(offer.getAmount());
        }

        @Test
        @DisplayName("스냅샷이 존재하지 않으면 예외를 던진다")
        void throwsException_whenSnapshotNotFound() {
            // given
            final CreateOfferCommand command = new CreateOfferCommand(1L, 1L, "title", "story", "delivery");

            stubMemberExists(1L);
            given(offerSnapshotRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> offerService.createOffer(command))
                    .isInstanceOf(BusinessException.class);

            verifyMemberExists(1L);
            verify(offerSnapshotRepository).findById(1L);
            verify(productPort, never()).getProduct(anyLong());
            verify(offerRepository, never()).save(any(Offer.class));
        }

        @Test
        @DisplayName("스냅샷 작성자가 아니면 예외를 던진다")
        void throwsException_whenWriterMismatch() {
            // given
            final OfferSnapshot snapshot = createSnapshot(1L, 2L, 10L, new BigDecimal("10000"));
            final CreateOfferCommand command = new CreateOfferCommand(999L, 1L, "title", "story", "delivery");
            final ProductInfo productInfo = new ProductInfo(
                    2L,
                    List.of(),
                    "상품명",
                    "브랜드",
                    new BigDecimal("10000"),
                    "MDL-001",
                    "카테고리",
                    LocalDate.now(),
                    0L,
                    "설명",
                    LocalDateTime.now()
            );

            stubMemberExists(999L);
            given(offerSnapshotRepository.findById(1L)).willReturn(Optional.of(snapshot));
            given(productPort.getProduct(anyLong())).willReturn(productInfo);
            given(offerRepository.save(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when & then
            assertThatThrownBy(() -> offerService.createOffer(command))
                    .isInstanceOf(BusinessException.class);

            verifyMemberExists(999L);
            verify(offerSnapshotRepository).findById(1L);
            verify(productPort).getProduct(anyLong());
            verify(offerRepository).save(any());
        }

        @Test
        @DisplayName("현재 상품 가격과 스냅샷 가격이 다르면 예외를 던진다")
        void throwsException_whenPriceMismatch() {
            // given
            final OfferSnapshot snapshot = createSnapshot(1L, 2L, 10L, new BigDecimal("10000"));
            final CreateOfferCommand command = new CreateOfferCommand(1L, 1L, "title", "story", "delivery");

            stubMemberExists(1L);
            given(offerSnapshotRepository.findById(1L)).willReturn(Optional.of(snapshot));
            given(productPort.getProduct(10L)).willReturn(
                    new ProductInfo(
                            2L,
                            List.of(),
                            "상품명",
                            "브랜드",
                            new BigDecimal("20000"),
                            "MODEL-001",
                            "카테고리",
                            LocalDate.of(2026, 1, 1),
                            0L,
                            "상품 설명",
                            LocalDateTime.now()
                    )
            );

            // when & then
            assertThatThrownBy(() -> offerService.createOffer(command))
                    .isInstanceOf(BusinessException.class);

            verifyMemberExists(1L);
            verify(offerSnapshotRepository).findById(1L);
            verify(productPort).getProduct(10L);
            verify(offerRepository, never()).save(any());
        }
    }

    private void stubMemberExists(final Long memberId) {
        willDoNothing()
                .given(memberPort)
                .validateMemberExists(memberId);
    }

    private void verifyMemberExists(final Long memberId) {
        verify(memberPort)
                .validateMemberExists(memberId);
    }

    private ProductInfo productInfo(final Long sellerId) {
        return new ProductInfo(
                sellerId,
                List.of(),
                "상품명",
                "브랜드",
                new BigDecimal("10000"),
                "MODEL-001",
                "카테고리",
                LocalDate.of(2026, 1, 1),
                0L,
                "상품 설명",
                LocalDateTime.now()
        );
    }

    private OfferSnapshot createSnapshot(
            final Long writerId,
            final Long sellerId,
            final Long productId,
            final BigDecimal price
    ) {
        return OfferSnapshot.create(sellerId, writerId, productId, "MODEL-001", price);
    }
}
