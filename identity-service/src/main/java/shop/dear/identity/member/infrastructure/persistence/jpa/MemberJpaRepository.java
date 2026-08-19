package shop.dear.identity.member.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.identity.member.domain.constract.SellerStatus;
import shop.dear.identity.member.domain.model.Member;

import java.time.LocalDateTime;
import java.util.List;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {

    boolean existsByNickname(final String nickname);

    @EntityGraph(attributePaths = "seller")
    List<Member> findBySeller_StatusAndSeller_WithdrawnAtBefore(
        SellerStatus sellerStatus,
        LocalDateTime withdrawnAtBefore
    );
}
