package shop.dear.identity.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.dear.identity.member.domain.model.Seller;
import shop.dear.identity.member.domain.repository.SellerRepository;
import shop.dear.identity.member.infrastructure.persistence.jpa.SellerJpaRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SellerRepositoryAdapter implements SellerRepository {

	private final SellerJpaRepository jpaRepository;

	@Override
	public Optional<Seller> findByMemberId(final long memberId) {
		return jpaRepository.findByMemberId(memberId);
	}
}
