package shop.dear.commerce.product.application.dto.command;

import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.commerce.product.domain.constant.ProductSaleType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateProductCommand(
    ProductSaleType saleType,
    List<ProductImageContentCommand> productImageContents,
    String brand,
    String name,
    BigDecimal price,
    String modelNumber,
    ProductCategory category,
    LocalDate releaseDate,
    String description
) {
    public record ProductImageContentCommand(
        int sortOrder,
        String url,
        String story
    ) {
    }
}
