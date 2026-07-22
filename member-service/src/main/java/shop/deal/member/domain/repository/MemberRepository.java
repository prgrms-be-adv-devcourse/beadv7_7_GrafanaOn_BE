package shop.deal.member.domain.repository;

import shop.deal.member.domain.model.Member;

public interface MemberRepository {

    Member save(Member member);
    boolean existsByNickname(String nickname);
}
