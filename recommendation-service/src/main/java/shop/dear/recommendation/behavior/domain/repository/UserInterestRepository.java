package shop.dear.recommendation.behavior.domain.repository;

import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.recommendation.behavior.domain.model.UserInterest;

import java.util.List;
import java.util.Optional;

public interface UserInterestRepository {
    UserInterest save(UserInterest userInterest);

    Optional<UserInterest> findByMemberIdAndCategory(Long memberId, ProductCategory category);
    List<UserInterest> findByMemberIdOrderByScoreDesc(Long memberId);
}
