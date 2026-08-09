package shop.dear.identity.member.domain.repository;

import shop.dear.identity.member.domain.model.Seller;

import java.util.Optional;

public interface SellerRepository {
	Optional<Seller> findByMemberId(long memberId);
}
