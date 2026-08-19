package shop.dear.identity.member.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.identity.member.domain.constract.SellerStatus;
import shop.dear.identity.member.domain.model.Member;

import java.time.LocalDateTime;
import java.util.List;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {

    boolean existsByNickname(final String nickname);

    /**
     * Member.seller는 @OneToOne의 역방향이라 LAZY로 선언해도 프록시가 만들어지지 않고
     * 건마다 추가 조회가 나간다. @EntityGraph로 페치 조인해 N+1을 막는다.
     */
    @EntityGraph(attributePaths = "seller")
    List<Member> findBySeller_StatusAndSeller_WithdrawnAtBefore(
        SellerStatus sellerStatus,
        LocalDateTime withdrawnAtBefore
    );
}
