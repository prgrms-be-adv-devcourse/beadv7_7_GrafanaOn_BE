package shop.dear.commerce.product.domain.repository;

import shop.dear.commerce.product.domain.model.Product;

public interface ProductRepository {
    Product save(final Product product);
}
