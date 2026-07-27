package shop.dear.commerce.product.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.commerce.product.domain.model.Product;

import java.util.List;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {
    boolean existsBySellerIdAndStatusIn(final Long sellerId, final List<ProductStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.id = :productId")
    void increaseViewCount(@Param("productId") Long productId);
}
