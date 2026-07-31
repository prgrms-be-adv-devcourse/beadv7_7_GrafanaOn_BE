package shop.dear.identity.member.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.identity.member.domain.model.Seller;

import java.util.Optional;

public interface SellerJpaRepository extends JpaRepository<Seller, Long> {

    Optional<Seller> findByMemberId(final long memberId);
}
