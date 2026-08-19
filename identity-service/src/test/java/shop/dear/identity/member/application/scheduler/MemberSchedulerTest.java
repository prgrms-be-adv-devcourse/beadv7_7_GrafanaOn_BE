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
 * 배치가 커밋한 결과를 확인해야 하므로 테스트 자체는 트랜잭션 없이 돌리고,
 * 데이터는 TransactionTemplate으로 직접 넣고 지운다.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class MemberSchedulerTest {

	private static final int ACCOUNT_RETENTION_YEARS = 5;
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
	@DisplayName("이관 - 대상이 여러 건이어도 한 번에 모두 처리되고, 재실행 시 중복 이관되지 않는다")
	void archiveAllTargetsAtOnce() {

		int targetCount = 5;
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

		//when
		long archived = memberScheduler.archiveAccounts(LocalDateTime.now().minusMonths(1));

		//then
		assertEquals(targetCount, archived);

		//조회 1회 + 건당 insert/update 2회. 페치 조인이 빠지면 건마다 조회가 1회씩 더 붙는다
		long expectedStatements = 1 + (long) targetCount * 2;

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
	@DisplayName("폐기 - 만료된 보관 계좌만 삭제되고 미만료 건은 남는다")
	void deleteExpiredAccounts() {

		int expiredCount = 5;

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
		long deleted = memberScheduler.deleteExpiredAccounts(LocalDateTime.now());

		//then
		assertEquals(expiredCount, deleted);

		transactionTemplate.executeWithoutResult(status -> {
			Long remaining = em.createQuery(
				"select count(a) from ArchivedAccount a", Long.class).getSingleResult();
			assertEquals(1, remaining);
			assertEquals(1, findArchivedAccounts(survivorId).size());
		});

		//두 번째 실행에서는 삭제할 대상이 없다
		assertEquals(0, memberScheduler.deleteExpiredAccounts(LocalDateTime.now()));
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
