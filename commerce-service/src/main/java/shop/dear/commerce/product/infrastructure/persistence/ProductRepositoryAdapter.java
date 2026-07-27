package shop.dear.commerce.product.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.commerce.product.domain.model.Product;
import shop.dear.commerce.product.domain.repository.ProductRepository;
import shop.dear.commerce.product.infrastructure.persistence.jpa.ProductJpaRepository;
import shop.dear.common.exception.BusinessException;

import java.util.List;

import static shop.dear.commerce.product.domain.exception.ProductErrorCode.INVALID_PRODUCT;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository productRepository;

    @Override
    public Product save(final Product product) {
        return productRepository.save(product);
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public Product findById(final Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(INVALID_PRODUCT));
    }

    @Override
    public void delete(final Product product) {
        productRepository.delete(product);
    }

    @Override
    public boolean existsBySellerIdAndStatusIn(final Long sellerId, final List<ProductStatus> statuses) {
        return productRepository.existsBySellerIdAndStatusIn(sellerId, statuses);
    }

    @Override
    public List<Product> findByIds(final List<Long> productIds) {
        return productRepository.findAllById(productIds);
    }
}
