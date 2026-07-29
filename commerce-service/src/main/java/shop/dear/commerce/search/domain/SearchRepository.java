package shop.dear.commerce.search.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Page는 String Data에서 제공하는 페이징된 조회 결과 묶음
// Page<SearchResult>는 목록과 페이지 정보를 함께 담아준다.
public interface SearchRepository {

    // 저장 이벤트 수신 시
    void save(SearchProduct product);

    // 삭제 이벤트 수신 시
    void deleteByProductId(Long productId);

    // 검색 요청이 들어왔을 때 상품명 LIKE 검색
    Page<SearchProduct> searchByProductName(
            String keyword,
            Pageable pageable
    );

    // 카테고리 정확히 일치
    Page<SearchProduct> searchByCategory(
            String category,
            Pageable pageable
    );

    // 스토리 내용 LIKE 검색
    Page<SearchProduct> searchByStoryContent(
            String keyword,
            Pageable pageable
    );
}
