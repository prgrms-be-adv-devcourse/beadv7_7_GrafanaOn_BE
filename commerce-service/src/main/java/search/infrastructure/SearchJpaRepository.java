package search.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

// 상품 이름으로 찾고 %상품명% 형식이며 대문자는 소문자로.
public interface SearchJpaRepository extends JpaRepository<Product, Long> {
    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}
