package shop.deal.identity.member.domain.repository;

import shop.deal.identity.member.domain.model.Member;

import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);
    boolean existsByNickname(String nickname);
    Optional<Member> findById(Long id);
}
