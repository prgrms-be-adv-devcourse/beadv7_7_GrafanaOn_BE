package shop.dear.commerce.product.application.dto;

public record PresignedUrlInfoDto(
    int sortOrder,
    String presignedUrl
) {
}
