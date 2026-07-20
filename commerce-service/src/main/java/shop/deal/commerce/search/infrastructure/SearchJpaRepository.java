package shop.deal.commerce.search.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

// 상품 이름으로 찾고 %상품명% 형식이며 대문자는 소문자로.
// Product 테이블을 전혀 참조하지 않고 search_product 테이블만 조회한다.
public interface SearchJpaRepository extends JpaRepository<SearchProductJpaEntity, Long> {
    Page<SearchProductJpaEntity> findByProductNameContainingIgnoreCase(String keyword, Pageable pageable);
}
