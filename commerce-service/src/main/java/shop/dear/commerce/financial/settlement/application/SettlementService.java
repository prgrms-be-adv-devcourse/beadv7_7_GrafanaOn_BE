package shop.dear.commerce.financial.settlement.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

	private final SettlementRepository settlementRepository;
	private final SettlementBatchRepository settlementBatchRepository;
	private final WalletPort walletPort;
	private final SettlementPolicyService settlementPolicyService;

	//특정기간의 정산완료 이력 조회 (건별조회)
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

	//회원의 정산예정금액
	public BigDecimal getNetAmount(Long memberId, YearMonth targetMonth) {

		Long walletId = getWalletId(memberId);

		return settlementBatchRepository.findByPeriodAndWalletId(targetMonth.toString(), walletId)
			.map(SettlementBatch::getNetAmount)
			.orElse(BigDecimal.ZERO);
	}

	//주문 확정 시 정산 건을 생성한다. 집계 누적은 일 단위 배치가 담당한다.
	@Transactional
	public void createSettlement(final FinishedOrderEvent event) {
		final OrderType orderType = OrderType.valueOf(event.orderType());

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

		final Long purchaseId = orderType == OrderType.PURCHASE
				? event.orderId()
				: null;

		final Long offerId = orderType == OrderType.OFFER
				? event.orderId()
				: null;

		if (settlementRepository.existsByPurchaseIdOrOfferId(purchaseId, offerId)) {
			return;
		}

		final Settlement settlement = Settlement.create(
			policy.getId(),
			walletId,
			purchaseId,
			offerId,
			event.amount(),
			feeAmount,
			netAmount
		);

		settlementRepository.save(settlement);
	}

	private Long getWalletId(Long memberId) {

		WalletInfo info = walletPort.getWalletId(memberId);

		return info.walletId();
	}
}
