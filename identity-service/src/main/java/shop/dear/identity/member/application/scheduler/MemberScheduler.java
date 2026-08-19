package shop.dear.identity.member.application.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.identity.member.domain.constract.SellerStatus;
import shop.dear.identity.member.domain.model.ArchivedAccount;
import shop.dear.identity.member.domain.model.Member;
import shop.dear.identity.member.domain.model.SellerAccountSnapshot;
import shop.dear.identity.member.domain.repository.ArchivedAccountRepository;
import shop.dear.identity.member.domain.repository.MemberRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MemberScheduler {

	private final MemberRepository memberRepository;
	private final ArchivedAccountRepository archivedAccountRepository;

	@Transactional
	public int archiveAccounts(final LocalDateTime baseDate) {

		List<Member> members = memberRepository.findArchiveTargets(
			SellerStatus.WITHDRAWN,
			baseDate
		);

		List<ArchivedAccount> accounts = new ArrayList<>(members.size());

		for (Member member : members) {

			SellerAccountSnapshot snapshot = member.archiveSeller();

			accounts.add(ArchivedAccount.create(
				snapshot.memberId(),
				snapshot.accountInfo(),
				snapshot.withdrawnAt()
			));
		}

		archivedAccountRepository.saveAll(accounts);

		return accounts.size();
	}

	@Transactional
	public int deleteExpiredAccounts(final LocalDateTime baseDate) {

		return archivedAccountRepository.deleteExpired(baseDate);
	}
}
