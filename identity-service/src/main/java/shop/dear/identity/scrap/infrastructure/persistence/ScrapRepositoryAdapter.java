package shop.dear.identity.scrap.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import shop.dear.identity.scrap.domain.model.Scrap;
import shop.dear.identity.scrap.domain.repository.ScrapRepository;
import shop.dear.identity.scrap.infrastructure.persistence.jpa.ScrapJpaRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ScrapRepositoryAdapter implements ScrapRepository {

    private final ScrapJpaRepository jpaRepository;

    @Override
    public Scrap save(final Scrap scrap) {
        return jpaRepository.save(scrap);
    }

    @Override
    public void deleteById(final Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Optional<Scrap> findByMemberIdAndProductId(Long memberId, Long productId) {
        return jpaRepository.findByMemberIdAndProductId(memberId, productId);
    }

    @Override
    public Page<Scrap> findByMemberId(Long memberId, Pageable pageable) {
        return jpaRepository.findByMemberId(memberId, pageable);
    }

    @Override
    public boolean existsByMemberIdAndProductId(Long memberId, Long productId) {
        return jpaRepository.existsByMemberIdAndProductId(memberId, productId);
    }
}
