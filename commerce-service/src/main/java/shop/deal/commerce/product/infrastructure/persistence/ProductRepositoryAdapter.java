package shop.deal.commerce.product.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.deal.commerce.product.domain.repository.ProductRepository;
import shop.deal.commerce.product.infrastructure.persistence.jpa.ProductImageJpaRepository;
import shop.deal.commerce.product.infrastructure.persistence.jpa.ProductJpaRepository;
import shop.deal.commerce.product.infrastructure.persistence.jpa.StoryJpaRepository;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository productRepository;
    private final ProductImageJpaRepository productImageRepository;
    private final StoryJpaRepository storyRepository;

}
