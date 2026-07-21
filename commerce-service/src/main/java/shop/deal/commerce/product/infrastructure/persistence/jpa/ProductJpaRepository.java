package shop.deal.commerce.product.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.deal.commerce.product.domain.model.Product;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {
}
