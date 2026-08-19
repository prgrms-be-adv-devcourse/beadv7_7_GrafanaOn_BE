package shop.dear.identity.member.application.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.identity.member.domain.constract.SellerStatus;
import shop.dear.identity.member.domain.model.ArchivedAccount;
import shop.dear.identity.member.domain.model.Seller;
import shop.dear.identity.member.domain.repository.ArchivedAccountRepository;
import shop.dear.identity.member.domain.repository.SellerSchedulerRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 청크 1건이 곧 트랜잭션 1건이다.
 * 루프는 {@link MemberScheduler}가 돌린다 - 같은 빈에서 자기 호출을 하면
 * 프록시를 타지 않아 전체가 한 트랜잭션으로 묶이기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class AccountArchiveProcessor {

	private final SellerSchedulerRepository sellerSchedulerRepository;
	private final ArchivedAccountRepository archivedAccountRepository;

	//계좌정보 이관
	@Transactional
	public int archiveAccountsByChunk(final LocalDateTime baseDate, final int chunkSize) {

		List<Seller> sellerList = sellerSchedulerRepository.findArchiveTargets(
			SellerStatus.WITHDRAWN,
			baseDate,
			chunkSize
		);

		List<ArchivedAccount> accounts = new ArrayList<>(sellerList.size());

		for (Seller seller : sellerList) {
			//archive()가 계좌정보를 비우므로 반드시 보관 레코드를 먼저 만든다
			accounts.add(ArchivedAccount.create(
				seller.getMemberId(),
				seller.getAccountInfo(),
				seller.getWithdrawnAt()
			));

			seller.archive();
		}

		sellerSchedulerRepository.saveAll(sellerList);
		archivedAccountRepository.saveAll(accounts);

		return accounts.size();
	}

	//계좌정보 폐기
	@Transactional
	public int deleteAccountsByChunk(final LocalDateTime baseDate, final int chunkSize) {

		return (int) archivedAccountRepository.deleteExpiredChunk(baseDate, chunkSize);
	}
}
