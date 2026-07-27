package shop.dear.commerce.product.presentation.dto.response;

import shop.dear.commerce.product.application.dto.GetProductDetailDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProductDetailResponse(
    Long sellerId,
    List<ImageInfo> images,
    String name,
    BigDecimal price,
    String modelNumber,
    String category,
    LocalDate releaseDate,
    String description
) {
    public record ImageInfo(
        int sortOrder,
        String url,
        String story
    ) {
    }

    public static ProductDetailResponse of(final GetProductDetailDto dto) {
        return new ProductDetailResponse(
            dto.sellerId(),
            dto.images().stream()
                .map(img -> new ImageInfo(
                    img.sortOrder(),
                    img.url(),
                    img.story()
                ))
                .toList(),
            dto.name(),
            dto.price(),
            dto.modelNumber(),
            dto.category(),
            dto.releaseDate(),
            dto.description()
        );
    }
}
