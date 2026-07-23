package shop.dear.identity.member.infrastructure.client.dto;

public record ProductApiData(
    String status,
    String imageUrl,
    String brand,
    String name,
    String price
) {
}
