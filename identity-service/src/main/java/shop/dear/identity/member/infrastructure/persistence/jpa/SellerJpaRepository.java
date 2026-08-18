package shop.dear.identity.member.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.identity.member.domain.constract.SellerStatus;
import shop.dear.identity.member.domain.model.Seller;

import java.time.LocalDateTime;
import java.util.List;

public interface SellerJpaRepository extends JpaRepository<Seller, Long> {

	List<Seller> findByStatusAndWithdrawnAtBefore(
		SellerStatus status,
		LocalDateTime withdrawnAtBefore
	);
}
