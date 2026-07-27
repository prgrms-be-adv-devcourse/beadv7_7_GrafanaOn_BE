package shop.dear.commerce.product.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.commerce.product.domain.model.Product;

import java.util.List;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {
    boolean existsBySellerIdAndStatusIn(final Long sellerId, final List<ProductStatus> statuses);
}
