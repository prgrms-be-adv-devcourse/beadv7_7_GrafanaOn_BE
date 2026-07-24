package shop.dear.identity.scrap.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.identity.scrap.domain.model.Scrap;

import java.util.List;
import java.util.Optional;

public interface ScrapJpaRepository extends JpaRepository<Scrap, Long> {

    Optional<Scrap> findByMemberIdAndProductId(Long memberId, Long productId);
    List<Scrap> findByMemberIdOrderByInsertedAt(Long memberId);
}
