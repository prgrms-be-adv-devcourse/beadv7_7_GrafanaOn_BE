package shop.deal.member.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.deal.member.domain.Member;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {
    boolean existsByEmail(final String email);
    boolean existsByNickname(final String nickname);
}
