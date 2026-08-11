package shop.dear.commerce.search.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.search.domain.SearchProduct;
import shop.dear.commerce.search.domain.SearchRepository;

// infrastructure에서 domain으로 참조. DIP 구현
// SearchJpaRepository를 DI 하는 중.
@Repository
@RequiredArgsConstructor
// 설정이 없거나 JPA로 명시돼있으면 이걸 Bean으로 등록한다. 만약 설정이 Elasticsearch이면 Bean으로 등록하지 않는다.
@ConditionalOnProperty(name = "search.engine", havingValue = "jpa", matchIfMissing = true)
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
