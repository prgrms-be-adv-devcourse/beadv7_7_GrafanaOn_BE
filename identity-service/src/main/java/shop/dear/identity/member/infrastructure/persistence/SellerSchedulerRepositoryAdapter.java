package shop.dear.identity.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import shop.dear.identity.member.domain.constract.SellerStatus;
import shop.dear.identity.member.domain.model.Seller;
import shop.dear.identity.member.domain.repository.SellerSchedulerRepository;
import shop.dear.identity.member.infrastructure.persistence.jpa.SellerJpaRepository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SellerSchedulerRepositoryAdapter implements SellerSchedulerRepository {


	private final SellerJpaRepository jpaRepository;

	/**
	 * 처리된 셀러는 상태가 ARCHIVED로 바뀌어 다음 조회 대상에서 빠지므로
	 * offset 없이 항상 첫 페이지만 조회하면 된다.
	 */
	@Override
	public List<Seller> findArchiveTargets(
		final SellerStatus status,
		final LocalDateTime withdrawnAtBefore,
		final int chunkSize
	) {

		return jpaRepository.findByStatusAndWithdrawnAtBefore(
			status,
			withdrawnAtBefore,
			PageRequest.of(0, chunkSize, Sort.by(Sort.Direction.ASC, "id"))
		);
	}

	@Override
	public void saveAll(List<Seller> sellers) {
		jpaRepository.saveAll(sellers);
	}
}
