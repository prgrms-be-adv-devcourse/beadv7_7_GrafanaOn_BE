package shop.dear.commerce.financial.settlement.application.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.financial.settlement.domain.model.Settlement;
import shop.dear.commerce.financial.settlement.domain.model.SettlementBatch;
import shop.dear.commerce.financial.settlement.domain.repository.SettlementBatchRepository;
import shop.dear.commerce.financial.settlement.domain.repository.SettlementRepository;

import java.time.YearMonth;
import java.util.List;

// 전일 생성된 정산을 집계 및 누적
@Component
@RequiredArgsConstructor
public class SettlementAccumulationProcessor {

	private final SettlementRepository settlementRepository;
	private final SettlementBatchRepository settlementBatchRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void accumulate(final YearMonth period, final Long walletId, final List<Settlement> settlements) {

		final SettlementBatch batch = settlementBatchRepository
			.findByPeriodAndWalletId(period.toString(), walletId)
			.orElseGet(() -> SettlementBatch.create(period, walletId));

		for (final Settlement settlement : settlements) {
			batch.accumulate(
				settlement.getGrossAmount(),
				settlement.getFeeAmount(),
				settlement.getNetAmount()
			);
			settlement.accumulated();
		}

		settlementBatchRepository.save(batch);
		settlementRepository.saveAll(settlements);
	}
}
