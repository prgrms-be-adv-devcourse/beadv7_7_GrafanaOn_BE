package shop.dear.commerce.product.domain.repository;

import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.commerce.product.domain.model.Product;

import java.util.List;

public interface ProductRepository {
    Product save(final Product product);
    List<Product> findAll();
    Product findById(final Long productId);
    void delete(final Product product);
    boolean existsBySellerIdAndStatusIn(final Long sellerId, final List<ProductStatus> statuses);
    List<Product> findByIds(final List<Long> ids);
    void increaseViewCount(final Long productId);
    boolean existsProduct(final Long productId);
    List<Product> findAllBySellerId(final Long sellerId);
}
