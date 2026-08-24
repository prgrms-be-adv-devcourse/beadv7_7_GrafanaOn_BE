package shop.dear.commerce.financial.settlement.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import shop.dear.commerce.financial.settlement.application.dto.SettlementInfo;
import shop.dear.commerce.financial.settlement.application.dto.WalletInfo;
import shop.dear.commerce.financial.settlement.application.port.WalletPort;
import shop.dear.commerce.financial.settlement.domain.constant.SettlementStatus;
import shop.dear.commerce.financial.settlement.domain.model.Settlement;
import shop.dear.commerce.financial.settlement.domain.model.SettlementBatch;
import shop.dear.commerce.financial.settlement.domain.repository.SettlementBatchRepository;
import shop.dear.commerce.financial.settlement.domain.repository.SettlementRepository;
import shop.dear.commerce.financial.settlementpolicy.application.SettlementPolicyService;
import shop.dear.commerce.financial.settlementpolicy.domain.model.SettlementPolicy;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.type.OrderType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

	private static final Long MEMBER_ID = 1L;
	private static final Long SELLER_ID = 2L;
	private static final Long WALLET_ID = 10L;
	private static final Long POLICY_ID = 100L;
	private static final YearMonth TARGET_MONTH = YearMonth.of(2026, 8);
	private static final LocalDate HISTORY_START_DATE = LocalDate.of(2026, 7, 1);
	private static final LocalDate HISTORY_END_DATE = LocalDate.of(2026, 7, 31);
	private static final LocalDateTime HISTORY_START_DATE_TIME = HISTORY_START_DATE.atStartOfDay();
	private static final LocalDateTime HISTORY_END_DATE_TIME = HISTORY_END_DATE.plusDays(1).atStartOfDay();

	@Mock
	private SettlementRepository settlementRepository;

	@Mock
	private SettlementBatchRepository settlementBatchRepository;

	@Mock
	private WalletPort walletPort;

	@Mock
	private SettlementPolicyService settlementPolicyService;

	@InjectMocks
	private SettlementService settlementService;

	// 헬퍼 함수
	private Settlement settlement(final Long id, final BigDecimal netAmount) {
		final Settlement settlement = Settlement.create(
				POLICY_ID,
				WALLET_ID,
				200L,
				null,
				netAmount,
				BigDecimal.ZERO,
				netAmount
		);

		ReflectionTestUtils.setField(settlement, "id", id);

		return settlement;
	}

	private SettlementPolicy policy(final BigDecimal feeRate, final BigDecimal fixedFee) {
		final SettlementPolicy policy = SettlementPolicy.create(feeRate, fixedFee);

		ReflectionTestUtils.setField(policy, "id", POLICY_ID);

		return policy;
	}

	private void stubWalletId(final Long memberId) {
		given(walletPort.getWalletId(memberId))
				.willReturn(new WalletInfo(memberId, WALLET_ID));
	}

	private FinishedOrderEvent finishedOrderEvent(final OrderType orderType, final Long orderId) {
		return new FinishedOrderEvent(
				orderId,
				MEMBER_ID,
				SELLER_ID,
				300L,
				new BigDecimal("10000.00"),
				orderType.name()
		);
	}

	@Nested
	@DisplayName("getHistory")
	class GetHistory {

		@Test
		@DisplayName("정산 완료 건을 조회해 정산 정보 목록으로 반환한다")
		void returnsCompletedSettlements() {
			// given
			stubWalletId(MEMBER_ID);
			given(settlementRepository.findByInsertedAtBetween(
					HISTORY_START_DATE_TIME, HISTORY_END_DATE_TIME, WALLET_ID, SettlementStatus.COMPLETED
			)).willReturn(List.of(
					settlement(1L, new BigDecimal("10000.00")),
					settlement(2L, new BigDecimal("5500.00"))
			));

			// when
			final List<SettlementInfo> history = settlementService.getHistory(
					MEMBER_ID,
					HISTORY_START_DATE,
					HISTORY_END_DATE
			);

			// then
			assertThat(history).hasSize(2);
			assertThat(history).extracting(SettlementInfo::id)
					.containsExactly(1L, 2L);
			assertThat(history).extracting(SettlementInfo::walletId)
					.containsOnly(WALLET_ID);
		}

		@Test
		@DisplayName("startDate 0시부터 endDate 다음날 0시 직전까지의 기간으로 조회한다")
		void queriesWithDateRangeConvertedToDateTime() {
			// given
			stubWalletId(MEMBER_ID);
			given(settlementRepository.findByInsertedAtBetween(
					any(), any(), any(), any()
			)).willReturn(List.of());

			// when
			settlementService.getHistory(MEMBER_ID, HISTORY_START_DATE, HISTORY_END_DATE);

			// then
			verify(settlementRepository).findByInsertedAtBetween(
					HISTORY_START_DATE_TIME,
					HISTORY_END_DATE_TIME,
					WALLET_ID,
					SettlementStatus.COMPLETED
			);
		}
	}

	@Nested
	@DisplayName("getNetAmount")
	class GetNetAmount {

		@Test
		@DisplayName("해당 월의 집계가 있으면 집계의 netAmount 를 반환한다")
		void returnsBatchNetAmount() {
			// given
			stubWalletId(MEMBER_ID);

			final SettlementBatch batch = SettlementBatch.create(TARGET_MONTH, WALLET_ID);
			batch.accumulate(new BigDecimal("10000.00"), new BigDecimal("1000.00"), new BigDecimal("9000.00"));

			given(settlementBatchRepository.findByPeriodAndWalletId(TARGET_MONTH.toString(), WALLET_ID))
					.willReturn(Optional.of(batch));

			// when
			final BigDecimal netAmount = settlementService.getNetAmount(MEMBER_ID, TARGET_MONTH);

			// then
			assertThat(netAmount).isEqualByComparingTo("9000.00");
		}

		@Test
		@DisplayName("해당 월의 집계가 없으면 0을 반환한다")
		void returnsZero_whenBatchNotFound() {
			// given
			stubWalletId(MEMBER_ID);

			given(settlementBatchRepository.findByPeriodAndWalletId(TARGET_MONTH.toString(), WALLET_ID))
					.willReturn(Optional.empty());

			// when
			final BigDecimal netAmount = settlementService.getNetAmount(MEMBER_ID, TARGET_MONTH);

			// then
			assertThat(netAmount).isEqualByComparingTo(BigDecimal.ZERO);
		}
	}

	@Nested
	@DisplayName("createSettlement")
	class CreateSettlement {

		@Test
		@DisplayName("PURCHASE 주문이면 purchaseId 를 채우고 offerId 는 비운 정산 건을 저장한다")
		void savesSettlementWithPurchaseId() {
			// given
			final FinishedOrderEvent event = finishedOrderEvent(OrderType.PURCHASE, 200L);

			given(settlementPolicyService.getOrCreateDefaultPolicy())
					.willReturn(policy(new BigDecimal("0.10"), BigDecimal.ZERO));
			stubWalletId(SELLER_ID);
			given(settlementRepository.existsByPurchaseId(200L))
					.willReturn(false);

			// when
			settlementService.createSettlement(event);

			// then
			final ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
			verify(settlementRepository).save(captor.capture());

			final Settlement settlement = captor.getValue();

			assertThat(settlement.getSettlementPolicyId()).isEqualTo(POLICY_ID);
			assertThat(settlement.getWalletId()).isEqualTo(WALLET_ID);
			assertThat(settlement.getPurchaseId()).isEqualTo(200L);
			assertThat(settlement.getOfferId()).isNull();
			assertThat(settlement.getGrossAmount()).isEqualByComparingTo("10000.00");
			assertThat(settlement.getFeeAmount()).isEqualByComparingTo("1000.00");
			assertThat(settlement.getNetAmount()).isEqualByComparingTo("9000.00");
		}

		@Test
		@DisplayName("OFFER 주문이면 offerId 를 채우고 purchaseId 는 비운 정산 건을 저장한다")
		void savesSettlementWithOfferId() {
			// given
			final FinishedOrderEvent event = finishedOrderEvent(OrderType.OFFER, 200L);

			given(settlementPolicyService.getOrCreateDefaultPolicy())
					.willReturn(policy(new BigDecimal("0.10"), BigDecimal.ZERO));
			stubWalletId(SELLER_ID);
			given(settlementRepository.existsByOfferId(200L))
					.willReturn(false);

			// when
			settlementService.createSettlement(event);

			// then
			final ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
			verify(settlementRepository).save(captor.capture());

			final Settlement settlement = captor.getValue();

			assertThat(settlement.getPurchaseId()).isNull();
			assertThat(settlement.getOfferId()).isEqualTo(200L);
		}

		@Test
		@DisplayName("정률 수수료에 고정 수수료를 더해 fee, net 금액을 계산한다")
		void calculatesFeeWithFixedFee() {
			// given
			final FinishedOrderEvent event = finishedOrderEvent(OrderType.PURCHASE, 200L);

			given(settlementPolicyService.getOrCreateDefaultPolicy())
					.willReturn(policy(new BigDecimal("0.05"), new BigDecimal("300.00")));
			stubWalletId(SELLER_ID);
			given(settlementRepository.existsByPurchaseId(200L))
					.willReturn(false);

			// when
			settlementService.createSettlement(event);

			// then
			final ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
			verify(settlementRepository).save(captor.capture());

			final Settlement settlement = captor.getValue();

			assertThat(settlement.getFeeAmount()).isEqualByComparingTo("800.00");
			assertThat(settlement.getNetAmount()).isEqualByComparingTo("9200.00");
		}

		@Test
		@DisplayName("이미 생성된 정산 건이면 저장하지 않는다")
		void doesNotSave_whenAlreadyExists() {
			// given
			final FinishedOrderEvent event = finishedOrderEvent(OrderType.PURCHASE, 200L);

			given(settlementPolicyService.getOrCreateDefaultPolicy())
					.willReturn(policy(new BigDecimal("0.10"), BigDecimal.ZERO));
			stubWalletId(SELLER_ID);
			given(settlementRepository.existsByPurchaseId(200L))
					.willReturn(true);

			// when
			settlementService.createSettlement(event);

			// then
			verify(settlementRepository, never()).save(any(Settlement.class));
		}
	}
}
