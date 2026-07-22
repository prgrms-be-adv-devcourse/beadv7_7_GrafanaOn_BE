package shop.deal.member.domain.repository;

import shop.deal.member.domain.model.Member;

import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);
    boolean existsByNickname(String nickname);
    Optional<Member> findById(Long id);
}
