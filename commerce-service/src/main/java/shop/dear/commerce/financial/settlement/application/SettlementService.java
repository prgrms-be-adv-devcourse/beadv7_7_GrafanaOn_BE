package shop.dear.commerce.financial.settlement.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.financial.settlementpolicy.application.SettlementPolicyService;
import shop.dear.commerce.financial.settlement.application.dto.SettlementInfo;
import shop.dear.commerce.financial.settlement.application.dto.WalletInfo;
import shop.dear.commerce.financial.settlement.application.port.SettlementEventPublisher;
import shop.dear.commerce.financial.settlement.application.port.WalletPort;
import shop.dear.commerce.financial.settlement.domain.constant.SettlementStatus;
import shop.dear.commerce.financial.settlement.domain.model.Settlement;
import shop.dear.commerce.financial.settlement.domain.repository.SettlementRepository;
import shop.dear.commerce.financial.settlementpolicy.domain.model.SettlementPolicy;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.event.order.OrderType;
import shop.dear.common.event.settlement.SettlementPayoutEvent;
import shop.dear.common.event.settlement.SettlementPayoutItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

	private final SettlementRepository settlementRepository;
	private final SettlementEventPublisher settlementEventPublisher;
	private final WalletPort walletPort;
	private final SettlementPolicyService settlementPolicyService;

	//특정기간의 정산완료 이력 조회
	public List<SettlementInfo> getHistory(
			Long memberId,
			LocalDate startDate,
			LocalDate endDate
	) {

		Long walletId = getWalletId(memberId);

		return settlementRepository.findByInsertedAtBetween(
						startDate.atStartOfDay(),
						endDate.plusDays(1).atStartOfDay(),
						walletId,
						SettlementStatus.COMPLETED
				)
				.stream()
				.map(SettlementInfo::from)
				.toList();
	}

	//회원의 정산예정금액(전월 1일 ~ 마지막일)
	public BigDecimal getNetAmount(Long memberId, YearMonth targetMonth) {

		Long walletId = getWalletId(memberId);

		//정산예금 netAmount 합계
		BigDecimal netAmount = settlementRepository.findByInsertedAtBetween(
						getStartDate(targetMonth),
						getEndDate(targetMonth),
						walletId,
						SettlementStatus.PENDING
				)
				.stream()
				.map(SettlementInfo::from)
				.map(SettlementInfo::netAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		return netAmount;
	}

	// 정산 대상 (walletId) 목록 조회
	public List<Long> getPayoutTargetWalletIds(final YearMonth targetMonth) {

		return settlementRepository.findWalletIdsByInsertedAtBetween(
				getStartDate(targetMonth),
				getEndDate(targetMonth),
				SettlementStatus.PENDING
		);
	}

	// 정산 대기 건을 COMPLETED 로 바꾸고, 이벤트 발행하여 wallet에서 예치금 충전처리
	@Transactional
	public void requestPayout(final Long walletId, final YearMonth targetMonth) {

		final List<Settlement> settlements = settlementRepository.findByInsertedAtBetween(
				getStartDate(targetMonth),
				getEndDate(targetMonth),
				walletId,
				SettlementStatus.PENDING
		);

		if (settlements.isEmpty()) {
			return;
		}

		//COMPLETED 처리
		settlements.forEach(Settlement::payout);
		settlementRepository.saveAll(settlements);

		final List<SettlementPayoutItem> items = settlements.stream()
				.map(settlement -> new SettlementPayoutItem(
						settlement.getId(),
						settlement.getNetAmount()
				))
				.toList();

		//이벤트 발행 (wallet에서 예치금 충전처리)
		settlementEventPublisher.publish(
				new SettlementPayoutEvent(walletId, items)
		);
	}

	// targetMonth 기준 전월 1일 0시
	public LocalDateTime getStartDate(YearMonth targetMonth) {
		return targetMonth.minusMonths(1)
				.atDay(1)
				.atStartOfDay();
	}

	// targetMonth 기준 전월 말일 다음날(=targetMonth 1일) 0시
	public LocalDateTime getEndDate(YearMonth targetMonth) {
		return targetMonth.atDay(1)
				.atStartOfDay();
	}

	public Long getWalletId(Long memberId) {

		WalletInfo info = walletPort.getWalletId(memberId);

		return info.walletId();
	}

	@Transactional
	public void createPendingSettlement(final FinishedOrderEvent event) {
		final SettlementPolicy policy =
				settlementPolicyService.getOrCreateDefaultPolicy();

		final Long walletId = getWalletId(event.sellerId());

		final BigDecimal feeAmount = event.amount()
				.multiply(policy.getFeeRate())
				.add(policy.getFixedFee())
				.setScale(2, RoundingMode.HALF_UP);

		final BigDecimal netAmount = event.amount()
				.subtract(feeAmount)
				.setScale(2, RoundingMode.HALF_UP);

		final Long purchaseId = event.orderType() == OrderType.PURCHASE
				? event.orderId()
				: null;

		final Long offerId = event.orderType() == OrderType.OFFER
				? event.orderId()
				: null;

		settlementRepository.save(Settlement.create(
				policy.getId(),
				walletId,
				purchaseId,
				offerId,
				event.amount(),
				feeAmount,
				netAmount
		));
	}
}
