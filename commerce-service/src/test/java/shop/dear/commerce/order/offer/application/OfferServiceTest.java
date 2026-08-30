package shop.dear.commerce.order.offer.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import shop.dear.commerce.order.offer.application.dto.CreateOfferCommand;
import shop.dear.commerce.order.offer.application.dto.CreateOfferSnapshotCommand;
import shop.dear.commerce.common.event.FinishedOrderEventPublisher;
import shop.dear.commerce.order.offer.application.port.OfferEventPublisher;
import shop.dear.commerce.order.offer.domain.constant.OfferStatus;
import shop.dear.commerce.order.offer.domain.model.Offer;
import shop.dear.commerce.order.offer.domain.repository.OfferRepository;
import shop.dear.commerce.order.offersnapshot.domain.model.OfferSnapshot;
import shop.dear.commerce.order.offersnapshot.domain.repository.OfferSnapshotRepository;
import shop.dear.commerce.order.offer.application.port.MemberPort;
import shop.dear.commerce.order.offer.application.port.ProductPort;
import shop.dear.commerce.order.offer.application.port.dto.ProductInfo;
import shop.dear.commerce.order.offer.domain.constant.OfferReleaseReason;
import shop.dear.common.event.financial.PaymentHoldRequestedEvent;
import shop.dear.common.event.financial.PaymentReleaseRequestedEvent;
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
    private FinishedOrderEventPublisher finishedOrderEventPublisher;

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
            given(offerRepository.findByProductIdAndStatusInOrderByInsertedAtDesc(
                    offer.getProductId(), List.of(OfferStatus.PENDING)))
                    .willReturn(List.of());

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

            verify(finishedOrderEventPublisher).publish(event);

            final ArgumentCaptor<PaymentRequestedEvent> paymentCaptor =
                    ArgumentCaptor.forClass(PaymentRequestedEvent.class);
            verify(offerEventPublisher).publish(paymentCaptor.capture());

            final PaymentRequestedEvent paymentEvent = paymentCaptor.getValue();
            assertThat(paymentEvent.orderId()).isEqualTo(offer.getId());
            assertThat(paymentEvent.memberId()).isEqualTo(offer.getBuyerId());
            assertThat(paymentEvent.amount()).isEqualTo(offer.getAmount());
        }

        @Test
        @DisplayName("오퍼 수락 시 같은 상품의 다른 PENDING 오퍼는 거절되고 예치금 해제 이벤트가 발행된다")
        void acceptsOfferAndReleasesOtherPendingOffers() {
            // given
            final Offer acceptedOffer = createPendingPaidOffer();
            final Offer otherOffer = Offer.create(
                    999L,
                    2L,
                    3L,
                    10L,
                    BigDecimal.valueOf(10000),
                    "title",
                    "story",
                    "delivery"
            );
            otherOffer.markPaid();
            ReflectionTestUtils.setField(acceptedOffer, "id", 1L);
            ReflectionTestUtils.setField(otherOffer, "id", 999L);
            given(offerRepository.findById(1L)).willReturn(Optional.of(acceptedOffer));
            given(offerRepository.findByProductIdAndStatusInOrderByInsertedAtDesc(
                    acceptedOffer.getProductId(), List.of(OfferStatus.PENDING)))
                    .willReturn(List.of(otherOffer));

            // when
            offerService.acceptOffer(1L, 2L);

            // then
            assertThat(otherOffer.getStatus()).isEqualTo(OfferStatus.REJECTED);
            verify(offerRepository).save(otherOffer);

            final ArgumentCaptor<PaymentReleaseRequestedEvent> releaseCaptor =
                    ArgumentCaptor.forClass(PaymentReleaseRequestedEvent.class);
            verify(offerEventPublisher).publish(releaseCaptor.capture());

            final PaymentReleaseRequestedEvent releaseEvent = releaseCaptor.getValue();
            assertThat(releaseEvent.orderId()).isEqualTo(otherOffer.getId());
            assertThat(releaseEvent.memberId()).isEqualTo(otherOffer.getBuyerId());
            assertThat(releaseEvent.amount()).isEqualTo(otherOffer.getAmount());
            assertThat(releaseEvent.reason()).isEqualTo(OfferReleaseReason.OFFER_OUTBID.name());
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
        @DisplayName("오퍼를 거절하면 상태가 REJECTED로 변경되고 예치금 해제 이벤트를 발행한다")
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

            final ArgumentCaptor<PaymentReleaseRequestedEvent> captor =
                    ArgumentCaptor.forClass(PaymentReleaseRequestedEvent.class);
            verify(offerEventPublisher).publish(captor.capture());

            final PaymentReleaseRequestedEvent event = captor.getValue();
            assertThat(event.orderId()).isEqualTo(offer.getId());
            assertThat(event.memberId()).isEqualTo(offer.getBuyerId());
            assertThat(event.amount()).isEqualTo(offer.getAmount());
            assertThat(event.reason()).isEqualTo(OfferReleaseReason.OFFER_REJECTED.name());
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
            final Pageable pageable = PageRequest.of(0, 10);
            given(productPort.getProduct(10L)).willReturn(productInfo(2L));
            given(offerRepository.findByProductIdOrderByInsertedAtDesc(10L, pageable))
                    .willReturn(new PageImpl<>(List.of(offer1, offer2), pageable, 2));

            // when
            final Page<Offer> offers = offerService.findOffersByProductId(2L, 10L, List.of(), pageable);

            // then
            assertThat(offers.getContent()).hasSize(2);
            assertThat(offers.getContent().get(0).getSellerId()).isEqualTo(2L);
            assertThat(offers.getContent().get(1).getSellerId()).isEqualTo(2L);
            verify(productPort).getProduct(10L);
            verify(offerRepository).findByProductIdOrderByInsertedAtDesc(10L, pageable);
        }

        @Test
        @DisplayName("상태 필터로 특정 상품의 오퍼 목록을 조회한다")
        void returnsOffersByStatus_whenSellerOwnsProduct() {
            // given
            final Offer offer1 = createPendingPaidOffer();
            final Offer offer2 = createPendingPaidOffer();
            offer2.reject();
            final Pageable pageable = PageRequest.of(0, 10);
            given(productPort.getProduct(10L)).willReturn(productInfo(2L));
            given(offerRepository.findByProductIdAndStatusInOrderByInsertedAtDesc(10L, List.of(OfferStatus.PENDING), pageable))
                    .willReturn(new PageImpl<>(List.of(offer1), pageable, 1));

            // when
            final Page<Offer> offers = offerService.findOffersByProductId(2L, 10L, List.of(OfferStatus.PENDING), pageable);

            // then
            assertThat(offers.getContent()).hasSize(1);
            assertThat(offers.getContent().get(0).getStatus()).isEqualTo(OfferStatus.PENDING);
            verify(productPort).getProduct(10L);
            verify(offerRepository).findByProductIdAndStatusInOrderByInsertedAtDesc(10L, List.of(OfferStatus.PENDING), pageable);
        }

        @Test
        @DisplayName("접수된 오퍼가 없으면 빈 목록을 반환한다")
        void returnsEmptyList_whenNoOffers() {
            // given
            final Pageable pageable = PageRequest.of(0, 10);
            given(productPort.getProduct(10L)).willReturn(productInfo(2L));
            given(offerRepository.findByProductIdOrderByInsertedAtDesc(10L, pageable))
                    .willReturn(new PageImpl<>(List.of(), pageable, 0));

            // when
            final Page<Offer> offers = offerService.findOffersByProductId(2L, 10L, List.of(), pageable);

            // then
            assertThat(offers.getContent()).isEmpty();
            verify(productPort).getProduct(10L);
            verify(offerRepository).findByProductIdOrderByInsertedAtDesc(10L, pageable);
        }

        @Test
        @DisplayName("상품 판매자가 아니면 예외를 던진다")
        void throwsException_whenNotSeller() {
            // given
            final Pageable pageable = PageRequest.of(0, 10);
            given(productPort.getProduct(10L)).willReturn(productInfo(999L));

            // when & then
            assertThatThrownBy(() -> offerService.findOffersByProductId(2L, 10L, List.of(), pageable))
                    .isInstanceOf(BusinessException.class);

            verify(productPort).getProduct(10L);
            verify(offerRepository, never()).findByProductIdOrderByInsertedAtDesc(10L, pageable);
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
                .validateMemberExists();
    }

    private void verifyMemberExists(final Long memberId) {
        verify(memberPort)
                .validateMemberExists();
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
