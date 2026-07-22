package shop.dear.commerce.product.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.commerce.product.domain.model.Product;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {
}
