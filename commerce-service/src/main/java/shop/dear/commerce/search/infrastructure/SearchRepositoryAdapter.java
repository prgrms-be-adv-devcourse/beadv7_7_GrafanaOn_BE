package shop.dear.commerce.search.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.search.domain.SearchProduct;
import shop.dear.commerce.search.domain.SearchRepository;

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
                .findByProductNameContainingIgnoreCase(keyword, withoutScoreSort(pageable))
                .map(SearchProductJpaEntity::toDomain);
    }

    @Override
    public Page<SearchProduct> searchByCategory(String category, Pageable pageable) {
        return searchJpaRepository
                .findByCategoryIgnoreCase(category, withoutScoreSort(pageable))
                .map(SearchProductJpaEntity::toDomain);
    }

    @Override
    public Page<SearchProduct> searchByStoryContent(String keyword, Pageable pageable) {
        return searchJpaRepository
                .findByStoryContentContainingIgnoreCase(keyword, withoutScoreSort(pageable))
                .map(SearchProductJpaEntity::toDomain);
    }

    /**
     * 관련도는 ES가 쿼리를 실행하면서 계산하는 값이기 때문에 JPA에는 대응하는 컬럼이 없다.
     * ES 장애로 이 어댑터가 폴백으로 불릴 때 그대로 넘기면 Spring Date JPA가 엔티티 속성으로 해석하려다 실패하게 된다.
     * 따라서 이 경우에는 최신순으로 해석하도록 조치한다.
     */
    private Pageable withoutScoreSort(final Pageable pageable) {
        boolean hasScoreSort = pageable.getSort().stream()
                .anyMatch(order -> "_score".equals(order.getProperty()));

        if (!hasScoreSort) {
            return pageable;
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "productInsertedAt")
        );
    }
}
