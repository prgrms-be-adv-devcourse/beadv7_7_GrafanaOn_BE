package shop.dear.commerce.search.application.dto;

public record SearchQuery(
        String keyword,
        SearchType type,
        SearchSort sort,
        int page,
        int size
) {
}