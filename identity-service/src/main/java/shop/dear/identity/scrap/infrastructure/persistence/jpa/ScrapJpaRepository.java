package shop.dear.identity.scrap.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.identity.scrap.domain.model.Scrap;

import java.util.Optional;

public interface ScrapJpaRepository extends JpaRepository<Scrap, Long> {

    Optional<Scrap> findByMemberIdAndProductId(Long memberId, Long productId);
    Page<Scrap> findByMemberId(Long memberId, Pageable pageable);
    boolean existsByMemberIdAndProductId(Long memberId, Long productId);
}
