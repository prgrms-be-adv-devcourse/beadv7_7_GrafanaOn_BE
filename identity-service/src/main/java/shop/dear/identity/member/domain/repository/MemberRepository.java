package shop.dear.identity.member.domain.repository;

import shop.dear.identity.member.domain.constract.SellerStatus;
import shop.dear.identity.member.domain.model.Member;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);
    boolean existsByNickname(String nickname);
    Optional<Member> findById(Long id);
    List<Member> findArchiveTargets(SellerStatus sellerStatus, LocalDateTime withdrawnAtBefore);
}
