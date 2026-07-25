package shop.dear.identity.scrap.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import shop.dear.identity.scrap.domain.model.Scrap;

import java.util.Optional;

public interface ScrapRepository {
    Scrap save(Scrap scrap);
    void deleteById(Long id);
    Optional<Scrap> findByMemberIdAndProductId(Long memberId, Long productId);
    Page<Scrap> findByMemberId(Long memberId, Pageable pageable);
    boolean existsByMemberIdAndProductId(Long memberId, Long productId);
}
