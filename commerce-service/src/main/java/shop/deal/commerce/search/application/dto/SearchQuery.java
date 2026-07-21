package shop.deal.commerce.search.application.dto;

public record SearchQuery(
        String keyword,
        SearchType type,
        SearchSort sort,
        int page,
        int size
) {
}