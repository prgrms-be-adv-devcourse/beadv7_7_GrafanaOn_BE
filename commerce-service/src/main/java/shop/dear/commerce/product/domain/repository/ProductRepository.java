package shop.dear.commerce.product.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.commerce.product.domain.constant.ProductSaleType;
import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.commerce.product.domain.model.Product;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductRepository {

    Product save(final Product product);

    List<Product> findAll();

    Product findById(final Long productId);

    boolean existsBySellerIdAndStatusIn(final Long sellerId, final List<ProductStatus> statuses);

    List<Product> findAllByIdIn(final List<Long> ids);

    void increaseViewCount(final Long productId);

    boolean existsById(final Long productId);

    Page<Product> findAllBySellerId(final Long sellerId, final Pageable pageable);

    Page<Product> findAllBySaleTypeAndStatusAndCreatedAtAndCategory(
        final ProductSaleType saleType,
        final ProductStatus status,
        final LocalDateTime startDate,
        final LocalDateTime endDate,
        final ProductCategory category,
        final Pageable pageable
    );

    int updateStatusToOnSale(final LocalDateTime startTime, final LocalDateTime endTime);

    Product findByIdWithPessimisticWrite(final Long productId);
  
}
