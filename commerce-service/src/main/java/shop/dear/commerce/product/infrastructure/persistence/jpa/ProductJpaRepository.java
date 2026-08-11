package shop.dear.commerce.product.infrastructure.persistence.jpa;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.commerce.product.domain.constant.ProductSaleType;
import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.commerce.product.domain.model.Product;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {
    boolean existsBySellerIdAndStatusIn(final Long sellerId, final List<ProductStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.id = :productId AND p.deletedAt IS NULL")
    void increaseViewCount(@Param("productId") Long productId);

    List<Product> findAllBySellerIdAndDeletedAtIsNull(final Long sellerId);

    @Query("""
        SELECT p FROM Product p
            WHERE (:saleType IS NULL OR p.saleType = :saleType)
            AND (:status IS NULL OR p.status = :status)
            AND (cast(:startDate as timestamp) IS NULL OR p.insertedAt >= :startDate)
            AND (cast(:endDate as timestamp) IS NULL OR p.insertedAt <= :endDate)
            AND (:category IS NULL OR p.category = :category)
            ORDER BY p.viewCount DESC, p.insertedAt DESC
    """)
    List<Product> findAllBySaleTypeAndStatusAndCreatedAtAndCategoryAndDeletedAtIsNull(
        @Param("saleType") final ProductSaleType saleType,
        @Param("status") final ProductStatus status,
        @Param("startDate") final LocalDateTime startDate,
        @Param("endDate") final LocalDateTime endDate,
        @Param("category") final ProductCategory category
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Product p SET p.status = 'ON_SALE'
        WHERE p.status = 'PREPARING'
          AND p.insertedAt >= :startTime
          AND p.insertedAt < :endTime
          AND p.deletedAt IS NULL
        """)
    int updateStatusToOnSale(@Param("startTime") final LocalDateTime startTime, @Param("endTime") final LocalDateTime endTime);

    List<Product> findAllByIdIn(final List<Long> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("select p from Product p where p.id = :productId")
    Optional<Product> findByIdWithPessimisticWrite(@Param("productId") final Long productId);
}
