package shop.dear.identity.member.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shop.dear.identity.member.application.scheduler.MemberScheduler;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringMemberScheduler {

	private final MemberScheduler memberScheduler;

	@Scheduled(cron = "0 0 2 2 * *", zone = "Asia/Seoul")
	public void archiveWithdrawnSellerAccounts() {

		long startedAt = System.currentTimeMillis();
		log.info("[Member Scheduler] 계좌정보 이관 시작");

		try {
			LocalDateTime baseDate = LocalDateTime.now().minusMonths(1);

			long size = memberScheduler.archiveAccounts(baseDate);

			log.info("[Member Scheduler] 계좌정보 이관 성공 - {}건, {}ms 소요",
				size, System.currentTimeMillis() - startedAt);

		} catch (Exception e) {
			log.error("[Member Scheduler] 계좌정보 이관 실패 - {}ms 소요",
				System.currentTimeMillis() - startedAt, e);
			throw e;
		}
	}

	@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
	public void deleteArchivedAccounts() {

		long startedAt = System.currentTimeMillis();
		log.info("[Member Scheduler] 계좌정보 폐기 시작");

		try {
			long size = memberScheduler.deleteExpiredAccounts(LocalDateTime.now());

			log.info("[Member Scheduler] 계좌정보 폐기 성공 - {}건, {}ms 소요",
				size, System.currentTimeMillis() - startedAt);

		} catch (Exception e) {
			log.error("[Member Scheduler] 계좌정보 폐기 실패 - {}ms 소요",
				System.currentTimeMillis() - startedAt, e);
		}
	}
}
