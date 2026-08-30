package shop.dear.recommendation.behavior.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.recommendation.behavior.domain.model.UserInterest;
import shop.dear.recommendation.behavior.domain.repository.UserInterestRepository;
import shop.dear.recommendation.behavior.infrastructure.persistence.jpa.UserInterestJpaRepository;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserInterestRepositoryAdapter implements UserInterestRepository {
    private final UserInterestJpaRepository userInterestJpaRepository;
    @Override
    public UserInterest save(UserInterest userInterest) {
        return userInterestJpaRepository.save(userInterest);
    }

    @Override
    public Optional<UserInterest> findByMemberIdAndCategory(Long memberId, ProductCategory category) {
        return userInterestJpaRepository.findByMemberIdAndCategory(memberId, category);
    }

    @Override
    public List<UserInterest> findByMemberIdOrderByScoreDesc(Long memberId) {
        return userInterestJpaRepository.findByMemberIdOrderByScoreDesc(memberId);
    }
}