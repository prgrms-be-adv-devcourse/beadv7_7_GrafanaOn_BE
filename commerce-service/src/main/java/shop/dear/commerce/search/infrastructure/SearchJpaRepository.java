package shop.dear.commerce.search.infrastructure;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

// 상품 이름으로 찾고 %상품명% 형식이며 대문자는 소문자로.
// Product 테이블을 전혀 참조하지 않고 search_product 테이블만 조회한다.
public interface SearchJpaRepository extends JpaRepository<SearchProductJpaEntity, Long> {
    // LIKE 검색
    Page<SearchProductJpaEntity> findByProductNameContainingIgnoreCase(
            String keyword,
            Pageable pageable
    );

    // 정확한 검색
    Page<SearchProductJpaEntity> findByCategoryIgnoreCase(
            String category,
            Pageable pageable
    );

    // LIKE 검색
    Page<SearchProductJpaEntity> findByStoryContentContainingIgnoreCase(
            String keyword,
            Pageable pageable
    );

    // 재색인은 전체를 순회하므로 오프셋 페이징을 쓰면 중간에 행이 삭제될 때 건너뛸 수 있다. 마지막으로 읽은 id 이후만 조회하는 키셋 방식 사용.
    // 메서드명이 너무 길어 명시적으로 Query 사용
    @Query("SELECT s FROM SearchProductJpaEntity s WHERE s.productId > :lastProductId ORDER by s.productId ASC")
    List<SearchProductJpaEntity> findNextChunk(@Param("lastProductId") Long lastProductId, Limit limit);

    // 재색인 도중 변경된 건을 보정하기 위해 사용한다.
    List<SearchProductJpaEntity> findAllByUpdatedAtGreaterThanEqual(LocalDateTime updatedAt);
}
