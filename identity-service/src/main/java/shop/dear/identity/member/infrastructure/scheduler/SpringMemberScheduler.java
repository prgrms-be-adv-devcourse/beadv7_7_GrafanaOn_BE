package shop.dear.identity.member.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shop.dear.identity.member.application.scheduler.MemberScheduler;

import java.time.LocalDateTime;

/**
 * 트랜잭션 경계는 {@link MemberScheduler}가 가진다.
 * 여기에 @Transactional을 두면 예외를 잡아 로깅만 하더라도
 * 이미 rollback-only로 마킹된 트랜잭션 때문에 UnexpectedRollbackException이 발생한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringMemberScheduler {

	private final MemberScheduler memberScheduler;

	//매월 2일 새벽 2시
	@Scheduled(cron = "0 0 2 2 * *", zone = "Asia/Seoul")
	public void archiveWithdrawnSellerAccounts() {

		long startedAt = System.currentTimeMillis();
		log.info("[Scheduler] 계좌정보 이관 시작");

		try {
			LocalDateTime baseDate = LocalDateTime.now().minusMonths(1);

			long size = memberScheduler.archiveAccounts(baseDate);

			log.info("[Scheduler] 계좌정보 이관 성공 - {}건, {}ms 소요",
				size, System.currentTimeMillis() - startedAt);

		} catch (Exception e) {
			log.error("[Scheduler] 계좌정보 이관 실패 - {}ms 소요",
				System.currentTimeMillis() - startedAt, e);
			throw e;
		}
	}

	//매일 새벽 3시 (이관 배치와 겹치지 않도록 분리)
	@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
	public void deleteArchivedAccounts() {

		long startedAt = System.currentTimeMillis();
		log.info("[Scheduler] 계좌정보 폐기 시작");

		try {

			long size = memberScheduler.deleteExpiredAccounts();

			log.info("[Scheduler] 계좌정보 폐기 성공 - {}건, {}ms 소요",
				size, System.currentTimeMillis() - startedAt);

		} catch (Exception e) {
			log.error("[Scheduler] 계좌정보 폐기 실패 - {}ms 소요",
				System.currentTimeMillis() - startedAt, e);
		}
	}
}
