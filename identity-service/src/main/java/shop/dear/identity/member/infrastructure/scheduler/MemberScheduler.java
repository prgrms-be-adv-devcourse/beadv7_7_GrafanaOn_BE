package shop.dear.identity.member.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.identity.member.domain.constract.SellerStatus;
import shop.dear.identity.member.domain.model.ArchivedAccount;
import shop.dear.identity.member.domain.model.Seller;
import shop.dear.identity.member.domain.repository.ArchivedAccountRepository;
import shop.dear.identity.member.domain.repository.SellerSchedulerRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberScheduler {

	private final SellerSchedulerRepository sellerSchedulerRepository;
	private final ArchivedAccountRepository archivedAccountRepository;

	@Transactional
	@Scheduled(cron = "0 0 3 2 * *", zone = "Asia/Seoul")
	public void archiveWithdrawnSellerAccounts() {

		long startedAt = System.currentTimeMillis();
		log.info("[Scheduler] 계좌정보 이관 시작");

		try {
			LocalDateTime baseDate = LocalDateTime.now().minusMonths(1);

			List<Seller> sellerList = sellerSchedulerRepository.findArchiveTargets(SellerStatus.WITHDRAWN, baseDate);

			List<ArchivedAccount> accounts = sellerList.stream()
				.map(ArchivedAccount::from)
				.toList();

			sellerSchedulerRepository.deleteAll(sellerList);
			archivedAccountRepository.saveAll(accounts);

			log.info("[Scheduler] 계좌정보 이관 성공 - {}건, {}ms 소요",
				sellerList.size(), System.currentTimeMillis() - startedAt);

		} catch (Exception e) {
			log.error("[Scheduler] 계좌정보 이관 실패 - {}ms 소요",
				System.currentTimeMillis() - startedAt, e);
			throw e;
		}
	}

	@Transactional
	@Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
	public void deleteArchivedAccounts() {

		long startedAt = System.currentTimeMillis();
		log.info("[Scheduler] 계좌정보 폐기 시작");

		try {
			LocalDateTime baseDate = LocalDateTime.now();

			List<ArchivedAccount> accountList = archivedAccountRepository.findByExpiresAtBefore(baseDate);

			archivedAccountRepository.deleteAll(accountList);

			log.info("[Scheduler] 계좌정보 폐기 성공 - {}건, {}ms 소요",
				accountList.size(), System.currentTimeMillis() - startedAt);

		} catch (Exception e) {
			log.error("[Scheduler] 계좌정보 폐기 실패 - {}ms 소요",
				System.currentTimeMillis() - startedAt, e);
		}
	}
}
