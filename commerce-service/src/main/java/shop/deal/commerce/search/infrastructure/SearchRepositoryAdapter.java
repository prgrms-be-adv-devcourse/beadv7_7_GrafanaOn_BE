package shop.deal.commerce.search.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import shop.deal.commerce.search.domain.SearchProduct;
import shop.deal.commerce.search.domain.SearchRepository;

// infrastructure에서 domain으로 참조. DIP 구현
// SearchJpaRepository를 DI 하는 중.
@Repository
@RequiredArgsConstructor
public class SearchRepositoryAdapter implements SearchRepository {
    private final SearchJpaRepository searchJpaRepository;


    @Override
    public void save(SearchProduct product) {
        searchJpaRepository.save(
                SearchProductJpaEntity.from(product)
        );
    }

    @Override
    public void deleteByProductId(Long productId) {
        searchJpaRepository.deleteById(productId);
    }

    @Override
    public Page<SearchProduct> searchByProductName(String keyword, Pageable pageable) {
        return searchJpaRepository
                .findByProductNameContainingIgnoreCase(keyword, pageable)
                .map(SearchProductJpaEntity::toDomain);
    }

    @Override
    public Page<SearchProduct> searchByCategory(String category, Pageable pageable) {
        return searchJpaRepository
                .findByCategoryIgnoreCase(category, pageable)
                .map(SearchProductJpaEntity::toDomain);
    }

    @Override
    public Page<SearchProduct> searchByStoryContent(String keyword, Pageable pageable) {
        return searchJpaRepository
                .findByStoryContentContainingIgnoreCase(keyword, pageable)
                .map(SearchProductJpaEntity::toDomain);
    }
}
