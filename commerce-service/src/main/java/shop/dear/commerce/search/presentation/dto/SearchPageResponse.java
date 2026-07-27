package shop.dear.commerce.search.presentation.dto;

import org.springframework.data.domain.Page;
import shop.dear.commerce.search.application.dto.SearchResult;

import java.util.List;

public record SearchPageResponse(
        List<SearchResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static SearchPageResponse from(final Page<SearchResult> searchResults) {
        return new SearchPageResponse(
                searchResults.getContent(),
                searchResults.getNumber() + 1, // 실제로는 0번부터 시작하니 1페이지부터 시작.
                searchResults.getSize(),
                searchResults.getTotalElements(),
                searchResults.getTotalPages(),
                searchResults.isFirst(),
                searchResults.isLast()
        );
    }
}
