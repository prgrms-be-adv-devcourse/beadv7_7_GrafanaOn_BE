package shop.dear.commerce.product.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.dear.commerce.product.domain.constant.ProductSaleType;
import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.commerce.product.domain.model.Product;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {
    boolean existsBySellerIdAndStatusIn(final Long sellerId, final List<ProductStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.id = :productId")
    void increaseViewCount(@Param("productId") Long productId);

    List<Product> findAllBySellerId(final Long sellerId);

    @Query("SELECT p FROM Product p " +
        "WHERE (:saleType IS NULL OR p.saleType = :saleType) " +
        "AND (:status IS NULL OR p.status = :status)" +
        "ORDER BY p.viewCount DESC, p.insertedAt DESC")
    List<Product> findAllBySaleTypeAndStatus(@Param("saleType") final ProductSaleType saleType, @Param("status") final ProductStatus status);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Product p SET p.status = 'ON_SALE'
        WHERE p.status = 'PREPARING'
          AND p.insertedAt >= :startTime
          AND p.insertedAt < :endTime
        """)
    int updateStatusToOnSale(@Param("startTime") final LocalDateTime startTime, @Param("endTime") final LocalDateTime endTime);
}
