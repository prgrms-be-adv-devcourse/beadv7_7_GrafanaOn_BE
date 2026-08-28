package shop.dear.commerce.financial.settlement.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.financial.settlement.application.port.SettlementEventPublisher;
import shop.dear.commerce.financial.settlement.domain.constant.SettlementStatus;
import shop.dear.commerce.financial.settlement.domain.exception.SettlementErrorCode;
import shop.dear.commerce.financial.settlement.domain.model.Settlement;
import shop.dear.commerce.financial.settlement.domain.model.SettlementBatch;
import shop.dear.commerce.financial.settlement.domain.model.SettlementSummary;
import shop.dear.commerce.financial.settlement.domain.repository.SettlementBatchRepository;
import shop.dear.commerce.financial.settlement.domain.repository.SettlementRepository;
import shop.dear.common.event.settlement.SettlementPayoutEvent;
import shop.dear.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

// 원본데이터와 검증하여 건수와 금액이 일치하면 wallet에 지급요청
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementBatchPayoutProcessor {

	private final SettlementRepository settlementRepository;
	private final SettlementBatchRepository settlementBatchRepository;
	private final SettlementEventPublisher settlementEventPublisher;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void process(final String period, final Long walletId, final YearMonth yearMonth) {

		final SettlementBatch batch = settlementBatchRepository.findByPeriodAndWalletId(period, walletId)
			.orElseThrow(() -> new BusinessException(SettlementErrorCode.SETTLEMENT_BATCH_NOT_FOUND));

		verify(batch, yearMonth);

		settlementBatchRepository.save(batch);
	}

	private void verify(final SettlementBatch batch, final YearMonth yearMonth) {

		final List<Settlement> pending = findPending(batch, yearMonth);

		if (matches(batch, yearMonth)) {
			payout(batch, pending, yearMonth);

			return;
		}

		log.warn("[Settlement] 집계 불일치, 재집계 후 재검증 - period={}, walletId={}, 집계={}건/{}, 원본={}건",
			yearMonth, batch.getWalletId(),
			batch.getSettlementCount(), batch.getNetAmount(), pending.size());

		reaccumulate(batch, pending);

		if (!matches(batch, yearMonth)) {
			log.error("[Settlement] 재검증 실패, 정산 실패 처리 - period={}, walletId={}, 재집계={}건/{}",
				yearMonth, batch.getWalletId(), batch.getSettlementCount(), batch.getNetAmount());

			//SettlementBatch FAIL, Settlement FAIL
			batch.revalidate();
			pending.forEach(Settlement::fail);
			settlementRepository.saveAll(pending);

			return;
		}

		log.info("[Settlement] 재집계 후 검증 통과 - period={}, walletId={}, {}건/{}",
			yearMonth, batch.getWalletId(), batch.getSettlementCount(), batch.getNetAmount());

		payout(batch, pending, yearMonth);
	}

	private void payout(
		final SettlementBatch batch,
		final List<Settlement> pending,
		final YearMonth yearMonth
	) {

		if (batch.getSettlementCount() == 0) {
			log.warn("[Settlement] 지급 대상 정산 건 없음, 지급 보류 - period={}, walletId={}",
				yearMonth, batch.getWalletId());

			return;
		}

		batch.paid();

		pending.forEach(Settlement::payout);
		settlementRepository.saveAll(pending);

		settlementEventPublisher.publish(new SettlementPayoutEvent(
			batch.getWalletId(),
			batch.getId(),
			batch.getPeriod(),
			batch.getNetAmount()
		));

		log.info("[Settlement] 지급 이벤트 발행 - period={}, walletId={}, batchId={}, {}건/{}",
			yearMonth, batch.getWalletId(), batch.getId(), batch.getSettlementCount(), batch.getNetAmount());
	}

	private void reaccumulate(final SettlementBatch batch, final List<Settlement> pending) {

		batch.reset();

		pending.forEach(settlement -> batch.accumulate(
			settlement.getGrossAmount(),
			settlement.getFeeAmount(),
			settlement.getNetAmount()
		));

		batch.close();
	}

	private List<Settlement> findPending(final SettlementBatch batch, final YearMonth yearMonth) {

		return settlementRepository.findByInsertedAtBetween(
			startOf(yearMonth),
			endOf(yearMonth),
			batch.getWalletId(),
			SettlementStatus.PENDING
		);
	}

	private boolean matches(final SettlementBatch batch, final YearMonth yearMonth) {

		final SettlementSummary origin = settlementRepository.summarizeByInsertedAtBetween(
			startOf(yearMonth),
			endOf(yearMonth),
			batch.getWalletId(),
			SettlementStatus.PENDING
		);

		return batch.matches(
			origin.count(),
			origin.grossAmount(),
			origin.feeAmount(),
			origin.netAmount()
		);
	}

	private LocalDateTime startOf(final YearMonth yearMonth) {
		return yearMonth.atDay(1).atStartOfDay();
	}

	private LocalDateTime endOf(final YearMonth yearMonth) {
		return yearMonth.plusMonths(1).atDay(1).atStartOfDay();
	}
}
