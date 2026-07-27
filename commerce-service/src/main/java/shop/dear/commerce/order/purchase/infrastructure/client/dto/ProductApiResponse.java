package shop.dear.commerce.order.purchase.infrastructure.client.dto;

public record ProductApiResponse<T>(
    String code,
    String message,
    T data
) {
}
