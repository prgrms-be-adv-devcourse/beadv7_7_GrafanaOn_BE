package shop.dear.identity.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.dear.identity.member.domain.constract.SellerStatus;
import shop.dear.identity.member.domain.model.AccountInfo;
import shop.dear.identity.member.domain.model.Seller;
import shop.dear.identity.member.domain.repository.SellerSchedulerRepository;
import shop.dear.identity.member.infrastructure.persistence.jpa.SellerJpaRepository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SellerSchedulerRepositoryAdapter implements SellerSchedulerRepository {


	private final SellerJpaRepository jpaRepository;

	@Override
	public List<Seller> findArchiveTargets(final SellerStatus status, final LocalDateTime withdrawnAtBefore) {

		return jpaRepository.findByStatusAndWithdrawnAtBefore(
			status,
			withdrawnAtBefore
		);
	}
}
