package shop.deal.commerce.product.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.deal.commerce.product.domain.model.Story;

public interface StoryJpaRepository extends JpaRepository<Story, Long> {
}
