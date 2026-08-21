package shop.dear.commerce.financial.settlement.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shop.dear.commerce.financial.settlement.application.SettlementService;

import java.time.YearMonth;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

	private final SettlementService settlementService;

	//매월 1일 오전 5시 실행
	@Scheduled(cron = "0 0 5 1 * * ")
	@SchedulerLock(name = "settlement_payout", lockAtMostFor = "30m", lockAtLeastFor = "1m")
	public void payoutTaskByCron() {

		YearMonth targetMonth = YearMonth.now();

		List<Long> walletList = settlementService.getPayoutTargetWalletIds(targetMonth);

		walletList.forEach(walletId -> {
			settlementService.requestPayout(walletId, targetMonth);
		});
	}
}
