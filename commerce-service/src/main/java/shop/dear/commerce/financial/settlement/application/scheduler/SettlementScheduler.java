package shop.dear.commerce.financial.settlement.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import shop.dear.commerce.financial.settlement.domain.constant.SettlementBatchStatus;
import shop.dear.commerce.financial.settlement.domain.constant.SettlementStatus;
import shop.dear.commerce.financial.settlement.domain.model.Settlement;
import shop.dear.commerce.financial.settlement.domain.model.SettlementBatch;
import shop.dear.commerce.financial.settlement.domain.repository.SettlementBatchRepository;
import shop.dear.commerce.financial.settlement.domain.repository.SettlementRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

	private final SettlementRepository settlementRepository;
	private final SettlementBatchRepository settlementBatchRepository;
	private final SettlementAccumulationProcessor settlementAccumulationProcessor;
	private final SettlementBatchCloseProcessor settlementBatchCloseProcessor;
	private final SettlementBatchPayoutProcessor settlementBatchPayoutProcessor;

	public void accumulate(final LocalDate targetDate) {

		final List<Settlement> settlements = settlementRepository.findAllByInsertedAtBetween(
			targetDate.atStartOfDay(),
			targetDate.plusDays(1).atStartOfDay(),
			SettlementStatus.CREATED
		);

		final YearMonth period = YearMonth.from(targetDate);

		final Map<Long, List<Settlement>> byWallet = settlements.stream()
			.collect(Collectors.groupingBy(Settlement::getWalletId));

		int failedWallets = 0;

		for (final Map.Entry<Long, List<Settlement>> entry : byWallet.entrySet()) {
			try {
				settlementAccumulationProcessor.accumulate(period, entry.getKey(), entry.getValue());
			} catch (final Exception exception) {
				failedWallets++;
				log.error("[Settlement] 일 집계 누적 실패, 다른 지갑 누적은 계속 진행 - targetDate={}, walletId={}",
					targetDate, entry.getKey(), exception);
			}
		}

		log.info("[Settlement] 일 집계 누적 - targetDate={}, 정산 {}건, 지갑 {}개, 실패 {}개",
			targetDate, settlements.size(), byWallet.size(), failedWallets);
	}

	public void close(final YearMonth yearMonth) {

		final List<SettlementBatch> settlementBatches = settlementBatchRepository.findByPeriodAndState(
			yearMonth.toString(),
			SettlementBatchStatus.PENDING
		);

		int failedWallets = 0;

		for (final SettlementBatch batch : settlementBatches) {
			try {
				settlementBatchCloseProcessor.close(batch.getPeriod(), batch.getWalletId());
			} catch (final Exception exception) {
				failedWallets++;
				log.error("[Settlement] 월 마감 실패, 다른 지갑 마감은 계속 진행 - period={}, walletId={}",
					yearMonth, batch.getWalletId(), exception);
			}
		}

		log.info("[Settlement] 월 마감 - period={}, {}건, 실패 {}개", yearMonth, settlementBatches.size(), failedWallets);
	}

	public void paid(final YearMonth yearMonth) {

		final List<SettlementBatch> settlementBatches = settlementBatchRepository.findByPeriodAndState(
			yearMonth.toString(),
			SettlementBatchStatus.CLOSE
		);

		int failedWallets = 0;

		for (final SettlementBatch batch : settlementBatches) {
			try {
				settlementBatchPayoutProcessor.process(batch.getPeriod(), batch.getWalletId(), yearMonth);
			} catch (final Exception exception) {
				failedWallets++;
				log.error("[Settlement] 지급 처리 실패, 다른 지갑 지급은 계속 진행 - period={}, walletId={}",
					yearMonth, batch.getWalletId(), exception);
			}
		}

		log.info("[Settlement] 지급 처리 - period={}, {}건, 실패 {}개", yearMonth, settlementBatches.size(), failedWallets);
	}
}
