package shop.deal.identity.member.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.deal.identity.member.domain.model.Member;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {

    boolean existsByNickname(final String nickname);
}
