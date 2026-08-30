package shop.dear.recommendation.behavior.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.recommendation.behavior.domain.model.UserInterest;

import java.util.List;
import java.util.Optional;

public interface UserInterestJpaRepository extends JpaRepository<UserInterest, Long> {
    Optional<UserInterest> findByMemberIdAndCategory(Long memberId, ProductCategory category);
    List<UserInterest> findByMemberIdOrderByScoreDesc(Long memberId);
}
