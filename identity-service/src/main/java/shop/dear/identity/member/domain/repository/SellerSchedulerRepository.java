package shop.dear.identity.member.domain.repository;

import shop.dear.identity.member.domain.constract.SellerStatus;
import shop.dear.identity.member.domain.model.Seller;

import java.time.LocalDateTime;
import java.util.List;

public interface SellerSchedulerRepository {

	List<Seller> findArchiveTargets(SellerStatus status, LocalDateTime withdrawnAtBefore, int chunkSize);
	void saveAll(List<Seller> sellers);
}
