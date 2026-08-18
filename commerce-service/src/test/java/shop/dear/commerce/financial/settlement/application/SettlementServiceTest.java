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
import shop.dear.commerce.financial.settlement.application.port.SettlementEventPublisher;
import shop.dear.commerce.financial.settlement.application.port.WalletPort;
import shop.dear.commerce.financial.settlement.domain.constant.SettlementStatus;
import shop.dear.commerce.financial.settlement.domain.model.Settlement;
import shop.dear.commerce.financial.settlement.domain.repository.SettlementRepository;
import shop.dear.commerce.financial.settlementpolicy.application.SettlementPolicyService;
import shop.dear.commerce.financial.settlementpolicy.domain.model.SettlementPolicy;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.type.OrderType;
import shop.dear.common.event.settlement.SettlementPayoutEvent;
import shop.dear.common.event.settlement.SettlementPayoutItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

	private static final Long MEMBER_ID = 1L;
	private static final Long WALLET_ID = 10L;
	private static final YearMonth TARGET_MONTH = YearMonth.of(2026, 8);
	private static final LocalDateTime START_DATE = LocalDateTime.of(2026, 7, 1, 0, 0);
	private static final LocalDateTime END_DATE = LocalDateTime.of(2026, 8, 1, 0, 0);
	private static final LocalDate HISTORY_START_DATE = LocalDate.of(2026, 7, 1);
	private static final LocalDate HISTORY_END_DATE = LocalDate.of(2026, 7, 31);
	private static final LocalDateTime HISTORY_START_DATE_TIME = HISTORY_START_DATE.atStartOfDay();
	private static final LocalDateTime HISTORY_END_DATE_TIME = HISTORY_END_DATE.plusDays(1).atStartOfDay();
	private static final Long SELLER_ID = 2L;
	private static final Long POLICY_ID = 100L;

	@Mock
	private SettlementRepository settlementRepository;

	@Mock
	private SettlementEventPublisher settlementEventPublisher;

	@Mock
	private WalletPort walletPort;

	@InjectMocks
	private SettlementService settlementService;

	@Mock
	private SettlementPolicyService settlementPolicyService;

	// 헬퍼 함수
	private Settlement settlement(final Long id, final BigDecimal netAmount) {
		final Settlement settlement = Settlement.create(
				100L,
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

	private SettlementPolicy defaultPolicy() {
		final SettlementPolicy policy = SettlementPolicy.create(
				new BigDecimal("0.10"),
				BigDecimal.ZERO
		);

		ReflectionTestUtils.setField(policy, "id", POLICY_ID);

		return policy;
	}

	private void stubWalletId() {
		given(walletPort.getWalletId(MEMBER_ID))
				.willReturn(new WalletInfo(MEMBER_ID, WALLET_ID));
	}

	@Nested
	@DisplayName("getNetAmount")
	class GetNetAmount {

		@Test
		@DisplayName("정산 대기 건이 여러 개면 netAmount 합계를 반환한다")
		void returnsSumOfNetAmount() {
			// given
			stubWalletId();
			given(settlementRepository.findByInsertedAtBetween(
					START_DATE, END_DATE, WALLET_ID, SettlementStatus.PENDING
			)).willReturn(List.of(
					settlement(1L, new BigDecimal("10000.00")),
					settlement(2L, new BigDecimal("5500.00")),
					settlement(3L, new BigDecimal("2000.00"))
			));

			// when
			final BigDecimal netAmount = settlementService.getNetAmount(MEMBER_ID, TARGET_MONTH);

			// then
			assertThat(netAmount).isEqualByComparingTo(new BigDecimal("17500.00"));
		}

		@Test
		@DisplayName("정산 대기 건이 없으면 0을 반환한다")
		void returnsZero_whenNoSettlement() {
			// given
			stubWalletId();
			given(settlementRepository.findByInsertedAtBetween(
					any(), any(), any(), any()
			)).willReturn(List.of());

			// when
			final BigDecimal netAmount = settlementService.getNetAmount(MEMBER_ID, TARGET_MONTH);

			// then
			assertThat(netAmount).isEqualByComparingTo(BigDecimal.ZERO);
		}

		@Test
		@DisplayName("정산월 1일 0시부터 다음 달 1일 0시까지의 기간으로 조회한다")
		void queriesWithTargetMonthPeriod() {
			// given
			stubWalletId();
			given(settlementRepository.findByInsertedAtBetween(
					any(), any(), any(), any()
			)).willReturn(List.of());

			// when
			settlementService.getNetAmount(MEMBER_ID, TARGET_MONTH);

			// then
			verify(settlementRepository).findByInsertedAtBetween(
					START_DATE,
					END_DATE,
					WALLET_ID,
					SettlementStatus.PENDING
			);
		}
	}

	@Nested
	@DisplayName("getHistory")
	class GetHistory {

		@Test
		@DisplayName("정산 완료 건을 조회해 정산 정보 목록으로 반환한다")
		void returnsCompletedSettlements() {
			// given
			stubWalletId();
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
			stubWalletId();
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
	@DisplayName("getPayoutTargetWalletIds")
	class GetPayoutTargetWalletIds {

		@Test
		@DisplayName("정산 대기 상태인 지갑 목록을 반환한다")
		void returnsPendingWalletIds() {
			// given
			given(settlementRepository.findWalletIdsByInsertedAtBetween(
					START_DATE, END_DATE, SettlementStatus.PENDING
			)).willReturn(List.of(10L, 20L, 30L));

			// when
			final List<Long> walletIds = settlementService.getPayoutTargetWalletIds(TARGET_MONTH);

			// then
			assertThat(walletIds).containsExactly(10L, 20L, 30L);
		}
	}

	@Nested
	@DisplayName("requestPayout")
	class RequestPayout {

		@Test
		@DisplayName("정산 대기 건을 COMPLETED 로 바꾸고 저장한다")
		void changesStatusToCompleted() {
			// given
			final Settlement first = settlement(1L, new BigDecimal("10000.00"));
			final Settlement second = settlement(2L, new BigDecimal("5500.00"));

			given(settlementRepository.findByInsertedAtBetween(
					START_DATE, END_DATE, WALLET_ID, SettlementStatus.PENDING
			)).willReturn(List.of(first, second));

			// when
			settlementService.requestPayout(WALLET_ID, TARGET_MONTH);

			// then
			assertThat(first.getState()).isEqualTo(SettlementStatus.COMPLETED);
			assertThat(second.getState()).isEqualTo(SettlementStatus.COMPLETED);

			verify(settlementRepository).saveAll(List.of(first, second));
		}

		@Test
		@DisplayName("정산 건의 id 와 netAmount 를 담은 지급 이벤트를 발행한다")
		void publishesPayoutEvent() {
			// given
			given(settlementRepository.findByInsertedAtBetween(
					START_DATE, END_DATE, WALLET_ID, SettlementStatus.PENDING
			)).willReturn(List.of(
					settlement(1L, new BigDecimal("10000.00")),
					settlement(2L, new BigDecimal("5500.00"))
			));

			// when
			settlementService.requestPayout(WALLET_ID, TARGET_MONTH);

			// then
			final ArgumentCaptor<SettlementPayoutEvent> captor =
					ArgumentCaptor.forClass(SettlementPayoutEvent.class);
			verify(settlementEventPublisher).publish(captor.capture());

			final SettlementPayoutEvent event = captor.getValue();

			assertThat(event.walletId()).isEqualTo(WALLET_ID);
			assertThat(event.items()).extracting(SettlementPayoutItem::settlementId)
					.containsExactly(1L, 2L);
			assertThat(event.totalAmount()).isEqualByComparingTo(new BigDecimal("15500.00"));
		}

		@Test
		@DisplayName("정산 대기 건이 없으면 저장도 이벤트 발행도 하지 않는다")
		void doesNothing_whenNoSettlement() {
			// given
			given(settlementRepository.findByInsertedAtBetween(
					START_DATE, END_DATE, WALLET_ID, SettlementStatus.PENDING
			)).willReturn(List.of());

			// when
			settlementService.requestPayout(WALLET_ID, TARGET_MONTH);

			// then
			verify(settlementRepository, never()).saveAll(anyList());
			verify(settlementEventPublisher, never()).publish(any(SettlementPayoutEvent.class));
		}
	}

	@Nested
	@DisplayName("getWalletId")
	class GetWalletId {

		@Test
		@DisplayName("회원의 지갑 식별자를 조회한다")
		void returnsWalletId() {
			// given
			stubWalletId();

			// when
			final Long walletId = settlementService.getWalletId(MEMBER_ID);

			// then
			assertThat(walletId).isEqualTo(WALLET_ID);
		}
	}

	@Test
	void createPendingSettlement_savesSettlementWithTenPercentFee() {
		// given
		final FinishedOrderEvent event = new FinishedOrderEvent(
				200L,
				MEMBER_ID,
				SELLER_ID,
				300L,
				new BigDecimal("10000.00"),
				OrderType.PURCHASE.name()
		);

		given(settlementPolicyService.getOrCreateDefaultPolicy())
				.willReturn(defaultPolicy());

		given(walletPort.getWalletId(SELLER_ID))
				.willReturn(new WalletInfo(SELLER_ID, WALLET_ID));

		// when
		settlementService.createPendingSettlement(event);

		// then
		final ArgumentCaptor<Settlement> captor =
				ArgumentCaptor.forClass(Settlement.class);

		verify(settlementRepository).save(captor.capture());

		final Settlement settlement = captor.getValue();

		assertThat(settlement.getSettlementPolicyId()).isEqualTo(POLICY_ID);
		assertThat(settlement.getWalletId()).isEqualTo(WALLET_ID);
		assertThat(settlement.getPurchaseId()).isEqualTo(200L);
		assertThat(settlement.getOfferId()).isNull();
		assertThat(settlement.getGrossAmount())
				.isEqualByComparingTo("10000.00");
		assertThat(settlement.getFeeAmount())
				.isEqualByComparingTo("1000.00");
		assertThat(settlement.getNetAmount())
				.isEqualByComparingTo("9000.00");
		assertThat(settlement.getState()).isEqualTo(SettlementStatus.PENDING);
	}
}
