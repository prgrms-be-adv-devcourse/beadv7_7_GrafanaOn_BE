package shop.dear.identity.member.application.scheduler;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.member.domain.constract.SellerStatus;
import shop.dear.identity.member.domain.exception.MemberErrorCode;
import shop.dear.identity.member.domain.model.AccountInfo;
import shop.dear.identity.member.domain.model.ArchivedAccount;
import shop.dear.identity.member.domain.model.Member;
import shop.dear.identity.member.domain.model.Seller;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 청크마다 트랜잭션이 커밋되어야 하므로 테스트 자체는 트랜잭션 없이 돌리고,
 * 데이터는 TransactionTemplate으로 직접 넣고 지운다.
 */
@SpringBootTest
@TestPropertySource(properties = {
	"member.scheduler.chunk-size=2",
	"spring.jpa.properties.hibernate.generate_statistics=true"
})
class MemberSchedulerTest {

	private static final int ACCOUNT_RETENTION_YEARS = 5;
	private static final int CHUNK_SIZE = 2;
	private static final String NICKNAME_PREFIX = "scheduler-test-";

	@Autowired
	private EntityManager em;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private MemberScheduler memberScheduler;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@BeforeEach
	void cleanUp() {
		transactionTemplate.executeWithoutResult(status -> {
			em.createQuery("delete from ArchivedAccount").executeUpdate();
			em.createQuery(
					"delete from Seller s where s.member in (select m from Member m where m.nickname like :prefix)")
				.setParameter("prefix", NICKNAME_PREFIX + "%")
				.executeUpdate();
			em.createQuery("delete from Member m where m.nickname like :prefix")
				.setParameter("prefix", NICKNAME_PREFIX + "%")
				.executeUpdate();
		});
	}

	@Test
	@DisplayName("탈퇴 셀러 계좌정보 이관 - seller 계좌정보가 비워지고 보유기간은 탈퇴 시점 기준으로 산정된다")
	void archiveAccounts() {

		LocalDateTime withdrawnAt = LocalDateTime.now()
			.minusMonths(2)
			.truncatedTo(ChronoUnit.SECONDS);

		Long memberId = createWithdrawnSeller(0, withdrawnAt);

		//when - 탈퇴 후 1개월이 지난 셀러가 이관 대상
		long archived = memberScheduler.archiveAccounts(LocalDateTime.now().minusMonths(1));

		//then
		assertEquals(1, archived);

		transactionTemplate.executeWithoutResult(status -> {
			//NOT NULL 제약 위반 없이 계좌정보가 비워져야 한다
			Seller seller = findSellerByMemberId(memberId);
			assertEquals(SellerStatus.ARCHIVED, seller.getStatus());
			assertNull(seller.getAccountInfo());

			//보유기간은 이관 시점이 아닌 탈퇴 시점 + 5년
			List<ArchivedAccount> accounts = findArchivedAccounts(memberId);
			assertEquals(1, accounts.size());
			assertEquals(
				withdrawnAt.plusYears(ACCOUNT_RETENTION_YEARS),
				accounts.get(0).getExpiresAt()
			);
			assertEquals("신한", accounts.get(0).getAccountInfo().getBank());
		});
	}

	@Test
	@DisplayName("이관 청크 - 청크 크기를 넘는 대상도 여러 번에 나눠 모두 처리된다")
	void archiveAccountsOverMultipleChunks() {

		int targetCount = CHUNK_SIZE * 2 + 1;
		LocalDateTime withdrawnAt = LocalDateTime.now()
			.minusMonths(2)
			.truncatedTo(ChronoUnit.SECONDS);

		List<Long> memberIds = new ArrayList<>();
		for (int i = 0; i < targetCount; i++) {
			memberIds.add(createWithdrawnSeller(i, withdrawnAt));
		}

		//when
		long archived = memberScheduler.archiveAccounts(LocalDateTime.now().minusMonths(1));

		//then - 남는 대상 없이 전부 처리
		assertEquals(targetCount, archived);

		transactionTemplate.executeWithoutResult(status -> {
			memberIds.forEach(memberId -> {
				assertEquals(SellerStatus.ARCHIVED, findSellerByMemberId(memberId).getStatus());
				assertEquals(1, findArchivedAccounts(memberId).size());
			});
		});

		//이미 처리된 대상은 다시 이관되지 않는다
		assertEquals(0, memberScheduler.archiveAccounts(LocalDateTime.now().minusMonths(1)));
	}

	@Test
	@DisplayName("이관 조회 - @EntityGraph 페치 조인으로 seller를 지연 로딩하지 않는다")
	void archiveTargetsAreFetchedWithoutNPlusOne() {

		int targetCount = 4;
		LocalDateTime withdrawnAt = LocalDateTime.now()
			.minusMonths(2)
			.truncatedTo(ChronoUnit.SECONDS);

		for (int i = 0; i < targetCount; i++) {
			createWithdrawnSeller(i, withdrawnAt);
		}

		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		//when - 청크 크기가 2이므로 조회 쿼리는 청크 수(3회, 마지막은 빈 청크)만큼만 나가야 한다
		long archived = memberScheduler.archiveAccounts(LocalDateTime.now().minusMonths(1));

		//then
		assertEquals(targetCount, archived);
		//청크당 조회 1회 + 건당 insert/update 2회. 페치 조인이 빠지면 건마다 조회가 1회씩 더 붙는다
		int chunkCount = targetCount / CHUNK_SIZE + 1;
		long expectedStatements = chunkCount + (long) targetCount * 2;

		assertEquals(expectedStatements, statistics.getPrepareStatementCount());
	}

	@Test
	@DisplayName("애그리거트 불변식 - 탈퇴하지 않은 셀러는 루트에서 이관을 거부한다")
	void archiveSellerRejectsActiveSeller() {

		Member member = Member.create(
			"테스트",
			"서울시 강남구",
			"010-1234-5678",
			NICKNAME_PREFIX + "active"
		);
		member.registerSeller(AccountInfo.of("신한", "1234567890"));

		BusinessException exception = assertThrows(BusinessException.class, member::archiveSeller);

		assertEquals(MemberErrorCode.SELLER_NOT_WITHDRAWN, exception.getErrorCode());
	}

	@Test
	@DisplayName("폐기 청크 - 만료된 보관 계좌만 청크 단위로 모두 삭제된다")
	void deleteExpiredAccountsOverMultipleChunks() {

		int expiredCount = CHUNK_SIZE * 2 + 1;

		//만료 대상: 탈퇴 후 보유기간(5년)이 이미 지난 셀러
		LocalDateTime expiredWithdrawnAt = LocalDateTime.now()
			.minusYears(ACCOUNT_RETENTION_YEARS)
			.minusDays(1)
			.truncatedTo(ChronoUnit.SECONDS);

		for (int i = 0; i < expiredCount; i++) {
			createWithdrawnSeller(i, expiredWithdrawnAt);
		}
		//미만료 대상 1건
		Long survivorId = createWithdrawnSeller(expiredCount, LocalDateTime.now().minusMonths(2));

		memberScheduler.archiveAccounts(LocalDateTime.now().minusMonths(1));

		//when
		long deleted = memberScheduler.deleteExpiredAccounts();

		//then
		assertEquals(expiredCount, deleted);

		transactionTemplate.executeWithoutResult(status -> {
			Long remaining = em.createQuery(
				"select count(a) from ArchivedAccount a", Long.class).getSingleResult();
			assertEquals(1, remaining);
			assertEquals(1, findArchivedAccounts(survivorId).size());
		});

		//두 번째 실행에서는 삭제할 대상이 없다
		assertEquals(0, memberScheduler.deleteExpiredAccounts());
	}

	private Long createWithdrawnSeller(final int index, final LocalDateTime withdrawnAt) {

		return transactionTemplate.execute(status -> {
			Member member = Member.create(
				"테스트",
				"서울시 강남구",
				"010-1234-5678",
				NICKNAME_PREFIX + index
			);
			member.registerSeller(AccountInfo.of("신한", "1234567890"));
			em.persist(member);
			em.flush();

			member.withdrawSeller();
			ReflectionTestUtils.setField(member.getSeller(), "withdrawnAt", withdrawnAt);
			em.flush();

			return member.getId();
		});
	}

	private Seller findSellerByMemberId(final Long memberId) {
		return em.createQuery(
				"select s from Seller s where s.member.id = :memberId", Seller.class)
			.setParameter("memberId", memberId)
			.getSingleResult();
	}

	private List<ArchivedAccount> findArchivedAccounts(final Long memberId) {
		return em.createQuery(
				"select a from ArchivedAccount a where a.memberId = :memberId", ArchivedAccount.class)
			.setParameter("memberId", memberId)
			.getResultList();
	}
}
