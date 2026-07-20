package search.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import search.application.dto.SearchResult;

// Page는 String Data에서 제공하는 페이징된 조회 결과 묶음
// Page<SearchResult>는 목록과 페이지 정보를 함께 담아준다.
public interface SearchRepository {
    Page<SearchResult> searchByProductName(String keyword, Pageable pageable);
}
