package shop.dear.commerce.product.application.dto;

import shop.dear.commerce.product.domain.model.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GetProductDetailDto(
    Long sellerId,
    List<ImageInfo> images,
    String name,
    String brand,
    BigDecimal price,
    String modelNumber,
    String category,
    LocalDate releaseDate,
    Long viewCount,
    String description,
    LocalDateTime insertedAt
) {
    public record ImageInfo(
        int sortOrder,
        String url,
        String story
    ) {
    }

    public static GetProductDetailDto of(final Product product) {
        final List<GetProductDetailDto.ImageInfo> images = product.getImages().stream()
            .map(img -> new GetProductDetailDto.ImageInfo(
                img.getSortOrder(),
                img.getUrl(),
                img.getStory().getContent()
            ))
            .toList();

        return new GetProductDetailDto(
            product.getSellerId(),
            images,
            product.getName(),
            product.getBrand(),
            product.getPrice().getValue(),
            product.getModelNumber(),
            product.getCategory().toString(),
            product.getReleaseDate(),
            product.getViewCount(),
            product.getDescription(),
            product.getInsertedAt()
        );
    }
}
