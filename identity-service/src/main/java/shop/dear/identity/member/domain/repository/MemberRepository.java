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

    /**
     * 계좌정보 보관 이관 대상 회원을 청크 단위로 조회한다.
     * Seller는 Member 애그리거트의 내부 엔티티이므로 루트인 Member로 조회한다.
     */
    List<Member> findArchiveTargets(SellerStatus sellerStatus, LocalDateTime withdrawnAtBefore, int chunkSize);
}
