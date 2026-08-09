package shop.dear.identity.member.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.identity.member.domain.model.Member;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {

    boolean existsByNickname(final String nickname);
}
