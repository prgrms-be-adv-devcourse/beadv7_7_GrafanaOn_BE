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
public class AccountArchiveProcessor {

	private final MemberRepository memberRepository;
	private final ArchivedAccountRepository archivedAccountRepository;

	//계좌정보 이관
	@Transactional
	public int archiveAccountsByChunk(final LocalDateTime baseDate, final int chunkSize) {

		List<Member> members = memberRepository.findArchiveTargets(
			SellerStatus.WITHDRAWN,
			baseDate,
			chunkSize
		);

		List<ArchivedAccount> accounts = new ArrayList<>(members.size());

		for (Member member : members) {
			//상태 전이와 계좌정보 스냅샷은 애그리거트 루트가 함께 처리한다
			SellerAccountSnapshot snapshot = member.archiveSeller();

			accounts.add(ArchivedAccount.create(
				snapshot.memberId(),
				snapshot.accountInfo(),
				snapshot.withdrawnAt()
			));
		}

		//members는 영속 상태이므로 셀러 상태 변경은 더티 체킹으로 반영된다
		archivedAccountRepository.saveAll(accounts);

		return accounts.size();
	}

	//계좌정보 폐기
	@Transactional
	public int deleteAccountsByChunk(final LocalDateTime baseDate, final int chunkSize) {

		return (int) archivedAccountRepository.deleteExpiredChunk(baseDate, chunkSize);
	}
}
