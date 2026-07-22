package shop.dear.commerce.product.domain.repository;

import shop.dear.commerce.product.domain.model.Product;

public interface ProductRepository {
    void save(final Product product);
}
