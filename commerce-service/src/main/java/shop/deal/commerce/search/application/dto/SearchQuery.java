package shop.deal.commerce.search.application.dto;

public record SearchQuery(
        String keyword,
        SearchSort sort,
        int page,
        int size
) {
}
