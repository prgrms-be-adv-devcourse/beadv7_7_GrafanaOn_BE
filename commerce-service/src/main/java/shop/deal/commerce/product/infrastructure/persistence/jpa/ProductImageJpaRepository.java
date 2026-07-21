package shop.deal.commerce.product.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.deal.commerce.product.domain.model.ProductImage;

public interface ProductImageJpaRepository extends JpaRepository<ProductImage, Long> {
}
