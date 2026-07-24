package shop.dear.identity.scrap.domain.repository;

import shop.dear.identity.scrap.domain.model.Scrap;

import java.util.List;
import java.util.Optional;

public interface ScrapRepository {
    Scrap save(Scrap scrap);
    void deleteById(Long id);
    Optional<Scrap> findByMemberIdAndProductId(Long memberId, Long productId);
    List<Scrap> findByMemberIdOrderByInsertedAt(Long memberId);
}
