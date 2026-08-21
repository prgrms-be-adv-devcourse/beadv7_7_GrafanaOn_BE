package shop.dear.commerce.financial.settlement.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.financial.settlement.application.port.SettlementEventPublisher;
import shop.dear.commerce.financial.settlement.domain.constant.SettlementBatchStatus;
import shop.dear.commerce.financial.settlement.domain.constant.SettlementStatus;
import shop.dear.commerce.financial.settlement.domain.model.Settlement;
import shop.dear.commerce.financial.settlement.domain.model.SettlementBatch;
import shop.dear.commerce.financial.settlement.domain.model.SettlementSummary;
import shop.dear.commerce.financial.settlement.domain.repository.SettlementBatchRepository;
import shop.dear.commerce.financial.settlement.domain.repository.SettlementRepository;
import shop.dear.common.event.settlement.SettlementPayoutEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

	private final SettlementRepository settlementRepository;
	private final SettlementBatchRepository settlementBatchRepository;
	private final SettlementEventPublisher settlementEventPublisher;

	//일 단위 집계 - 해당 일자의 CREATED 정산 건을 월별 집계에 누적하고 PENDING 으로 넘긴다.
	@Transactional
	public void accumulate(final LocalDate targetDate) {

		final List<Settlement> settlements = settlementRepository.findAllByInsertedAtBetween(
			targetDate.atStartOfDay(),
			targetDate.plusDays(1).atStartOfDay(),
			SettlementStatus.CREATED
		);

		final YearMonth period = YearMonth.from(targetDate);

		//지갑별 집계를 한 번만 조회하도록 모아 둔다.
		final Map<Long, SettlementBatch> batches = new HashMap<>();

		for (final Settlement settlement : settlements) {

			final SettlementBatch batch = batches.computeIfAbsent(
				settlement.getWalletId(),
				walletId -> settlementBatchRepository
					.findByPeriodAndWalletId(period.toString(), walletId)
					.orElseGet(() -> SettlementBatch.create(period, walletId))
			);

			batch.accumulate(
				settlement.getGrossAmount(),
				settlement.getFeeAmount(),
				settlement.getNetAmount()
			);

			settlement.accumulated();
		}

		settlementBatchRepository.saveAll(List.copyOf(batches.values()));
		settlementRepository.saveAll(settlements);

		log.info("[Settlement] 일 집계 누적 - targetDate={}, 정산 {}건, 지갑 {}개",
			targetDate, settlements.size(), batches.size());
	}

	//마감처리
	@Transactional
	public void close(final YearMonth yearMonth) {

		final List<SettlementBatch> settlementBatches = settlementBatchRepository.findByPeriodAndState(
			yearMonth.toString(),
			SettlementBatchStatus.PENDING
		);

		settlementBatches.forEach(SettlementBatch::close);

		settlementBatchRepository.saveAll(settlementBatches);

		log.info("[Settlement] 월 마감 - period={}, {}건", yearMonth, settlementBatches.size());
	}

	//지급처리
	@Transactional
	public void paid(final YearMonth yearMonth) {

		final List<SettlementBatch> settlementBatches = settlementBatchRepository.findByPeriodAndState(
			yearMonth.toString(),
			SettlementBatchStatus.CLOSE
		);

		settlementBatches.forEach(batch -> verify(batch, yearMonth));

		settlementBatchRepository.saveAll(settlementBatches);
	}

	//검증 - 집계와 PENDING 정산 원본의 건수/금액을 비교한다.
	//불일치하면 집계를 비우고 원본에서 다시 누적한 뒤 한 번 더 검증한다.
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

	//지급 - 집계와 정산 건을 지급 완료로 바꾸고 지갑 충전 이벤트를 발행한다.
	//paid() 가 CLOSE 에서만 허용되므로 같은 집계가 두 번 발행되지 않는다.
	private void payout(
		final SettlementBatch batch,
		final List<Settlement> pending,
		final YearMonth yearMonth
	) {

		//지급할 금액이 없는 집계는 지급 대상이 아니다. CLOSE 로 남겨 확인할 수 있게 한다.
		if (batch.getSettlementCount() == 0) {
			log.warn("[Settlement] 지급 대상 정산 건 없음, 지급 보류 - period={}, walletId={}",
				yearMonth, batch.getWalletId());

			return;
		}

		batch.paid();

		pending.forEach(Settlement::payout);
		settlementRepository.saveAll(pending);

		//지급 단위가 집계이므로 검증을 통과한 집계 금액을 그대로 보낸다.
		settlementEventPublisher.publish(new SettlementPayoutEvent(
			batch.getWalletId(),
			batch.getId(),
			batch.getPeriod(),
			batch.getNetAmount()
		));

		log.info("[Settlement] 지급 이벤트 발행 - period={}, walletId={}, batchId={}, {}건/{}",
			yearMonth, batch.getWalletId(), batch.getId(), batch.getSettlementCount(), batch.getNetAmount());
	}

	//재집계 - 집계값을 비우고 PENDING 원본을 다시 순회하며 누적을 재실행한다.
	private void reaccumulate(final SettlementBatch batch, final List<Settlement> pending) {

		batch.reset();

		pending.forEach(settlement -> batch.accumulate(
			settlement.getGrossAmount(),
			settlement.getFeeAmount(),
			settlement.getNetAmount()
		));

		batch.close();
	}

	//해당 지갑, 해당 월의 지급 대기 정산 건
	private List<Settlement> findPending(final SettlementBatch batch, final YearMonth yearMonth) {

		return settlementRepository.findByInsertedAtBetween(
			startOf(yearMonth),
			endOf(yearMonth),
			batch.getWalletId(),
			SettlementStatus.PENDING
		);
	}

	//원본 합계와 집계값이 일치하는지 - 건수와 금액을 모두 대조한다.
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

	// period 1일 0시
	private LocalDateTime startOf(final YearMonth yearMonth) {
		return yearMonth.atDay(1).atStartOfDay();
	}

	// 다음 달 1일 0시
	private LocalDateTime endOf(final YearMonth yearMonth) {
		return yearMonth.plusMonths(1).atDay(1).atStartOfDay();
	}
}
