package shop.dear.commerce.product.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.product.domain.constant.ProductSaleType;
import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.commerce.product.domain.model.Product;
import shop.dear.commerce.product.domain.repository.ProductRepository;
import shop.dear.commerce.product.infrastructure.persistence.jpa.ProductJpaRepository;
import shop.dear.common.exception.BusinessException;

import java.time.LocalDateTime;
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
    public List<Product> findAllById(final List<Long> productIds) {
        return productRepository.findAllById(productIds);
    }

    @Override
    public void increaseViewCount(final Long productId) {
        productRepository.increaseViewCount(productId);
    }

    @Override
    public boolean existsById(final Long productId) {
        return productRepository.existsById(productId);
    }

    @Override
    public List<Product> findAllBySellerId(final Long sellerId) {
        return productRepository.findAllBySellerId(sellerId);
    }

    @Override
    public List<Product> findAllBySaleTypeAndStatus(final ProductSaleType saleType, final ProductStatus status) {
        return productRepository.findAllBySaleTypeAndStatus(saleType, status);
    }

    @Override
    public int updateStatusToOnSale(final LocalDateTime startTime, final LocalDateTime endTime) {
        return productRepository.updateStatusToOnSale(startTime, endTime);
    }
}
