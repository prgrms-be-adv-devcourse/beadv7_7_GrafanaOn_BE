package shop.dear.identity.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import shop.dear.identity.member.domain.constract.SellerStatus;
import shop.dear.identity.member.domain.model.Member;
import shop.dear.identity.member.domain.repository.MemberRepository;
import shop.dear.identity.member.infrastructure.persistence.jpa.MemberJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryAdapter implements MemberRepository {

    private final MemberJpaRepository jpaRepository;

    @Override
    public Member save(final Member member) {
        return jpaRepository.save(member);
    }

    @Override
    public boolean existsByNickname(final String nickname) {
        return jpaRepository.existsByNickname(nickname);
    }

    @Override
    public Optional<Member> findById(final Long id) {
        return jpaRepository.findById(id);
    }

    /**
     * 처리된 셀러는 상태가 ARCHIVED로 바뀌어 다음 조회 대상에서 빠지므로
     * offset 없이 항상 첫 페이지만 조회하면 된다.
     */
    @Override
    public List<Member> findArchiveTargets(
        final SellerStatus sellerStatus,
        final LocalDateTime withdrawnAtBefore,
        final int chunkSize
    ) {
        return jpaRepository.findBySeller_StatusAndSeller_WithdrawnAtBefore(
            sellerStatus,
            withdrawnAtBefore,
            PageRequest.of(0, chunkSize, Sort.by(Sort.Direction.ASC, "id"))
        );
    }
}
