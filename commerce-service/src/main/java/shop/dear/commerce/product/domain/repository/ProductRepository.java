package shop.dear.commerce.product.domain.repository;

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
    List<Product> findAllBySellerIdAndDeletedAtIsNull(final Long sellerId);
    List<Product> findAllBySaleTypeAndStatusAndCreatedAtAndDeletedAtIsNull(final ProductSaleType saleType, final ProductStatus status, final LocalDateTime startDate, final LocalDateTime endDate);
    int updateStatusToOnSale(final LocalDateTime startTime, final LocalDateTime endTime);
  
}
