package shop.dear.commerce.financial.settlement.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shop.dear.commerce.financial.settlement.application.scheduler.SettlementScheduler;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringSettlementScheduler {

	private final SettlementScheduler settlementScheduler;

	//매일 새벽 1시 - 전날 생성된 정산 건을 월별 집계에 누적한다.
	@Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
	@SchedulerLock(name = "settlement_accumulate", lockAtMostFor = "30m", lockAtLeastFor = "1m")
	public void accumulateDaily() {

		LocalDate targetDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);

		settlementScheduler.accumulate(targetDate);
	}

	//매월 1일 오전 2시 - 전월 집계를 마감한다.
	@Scheduled(cron = "0 0 2 1 * *", zone = "Asia/Seoul")
	@SchedulerLock(name = "settlement_close", lockAtMostFor = "30m", lockAtLeastFor = "1m")
	public void closeMonth() {

		YearMonth targetMonth = YearMonth.now(ZoneId.of("Asia/Seoul")).minusMonths(1);

		settlementScheduler.close(targetMonth);
	}

	//매월 1일 오전 4시 - 마감된 전월 집계를 검증하고 지급한다.
	@Scheduled(cron = "0 0 5 1 * *", zone = "Asia/Seoul")
	@SchedulerLock(name = "settlement_payout", lockAtMostFor = "30m", lockAtLeastFor = "1m")
	public void payoutSettlement() {

		YearMonth targetMonth = YearMonth.now(ZoneId.of("Asia/Seoul")).minusMonths(1);

		settlementScheduler.paid(targetMonth);
	}
}
