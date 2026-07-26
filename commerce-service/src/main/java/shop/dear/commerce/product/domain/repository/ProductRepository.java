package shop.dear.commerce.product.domain.repository;

import shop.dear.commerce.product.domain.model.Product;

import java.util.List;

public interface ProductRepository {
    Product save(final Product product);
    List<Product> findAll();
    Product findById(final Long productId);
}
