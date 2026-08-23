package shop.dear.commerce.financial.settlement.application.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.financial.settlement.domain.exception.SettlementErrorCode;
import shop.dear.commerce.financial.settlement.domain.model.SettlementBatch;
import shop.dear.commerce.financial.settlement.domain.repository.SettlementBatchRepository;
import shop.dear.common.exception.BusinessException;

// 전월 정산을 마감처리(accumulation 메서드에서 현재 batch 상태를 검증하므로 close 상태일 때는 더이상 누적 처리 불가)
@Component
@RequiredArgsConstructor
public class SettlementBatchCloseProcessor {

	private final SettlementBatchRepository settlementBatchRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void close(final String period, final Long walletId) {

		final SettlementBatch batch = settlementBatchRepository.findByPeriodAndWalletId(period, walletId)
			.orElseThrow(() -> new BusinessException(SettlementErrorCode.SETTLEMENT_BATCH_NOT_FOUND));

		batch.close();

		settlementBatchRepository.save(batch);
	}
}
