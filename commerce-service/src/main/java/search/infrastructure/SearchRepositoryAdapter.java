package search.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import search.application.dto.SearchResult;
import search.domain.SearchRepository;

// infrastructure에서 domain으로 참조. DIP 구현
// SearchJpaRepository를 DI 하는 중.
@Repository
@RequiredArgsConstructor
public class SearchRepositoryAdapter implements SearchRepository {
    private final SearchJpaRepository searchJpaRepository;

    @Override
    public Page<SearchResult> searchByProductName(String keyword, Pageable pageable) {
        return searchJpaRepository.findByNameContainingIgnoreCase(keyword, pageable)
                .map(this::toSearchResult);
    }

    // Product에서 정한 변수를 따라가서 product getter해와야 한다.
    private SearchResult toSearchResult(Product product) {
        return new SearchResult(
                product.getName(),
                product.getModelNumber(),
                product.getCategory(),
                product.getReleaseDate(),
                product.getPrice(),
                product.getSaleType(),
                product.getViewCount(),
                product.getDescription()
        );
    }
}
