package shop.dear.commerce.product.application.dto;

public record PresignedUrlInfo(
    int sortOrder,
    String presignedUrl
) {
}
