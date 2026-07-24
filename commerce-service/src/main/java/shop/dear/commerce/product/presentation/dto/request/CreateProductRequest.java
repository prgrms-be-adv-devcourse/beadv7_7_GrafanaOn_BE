package shop.dear.commerce.product.presentation.dto.request;

import shop.dear.commerce.product.application.dto.command.CreateProductCommand;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.commerce.product.domain.constant.ProductSaleType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateProductRequest(
    ProductSaleType saleType,
    List<ProductImageContentRequest> productImageContents,
    String brand,
    String name,
    BigDecimal price,
    String modelNumber,
    ProductCategory category,
    LocalDate releaseDate,
    String description
) {
    public record ProductImageContentRequest(
        int sortOrder,
        String url,
        String story
    ) {
    }

    public CreateProductCommand toCommand() {
        final List<CreateProductCommand.ProductImageContentCommand> productImageContents = this.productImageContents.stream()
            .map(img -> new CreateProductCommand.ProductImageContentCommand(
                img.sortOrder,
                img.url,
                img.story
            ))
            .toList();

        return new CreateProductCommand(
            this.saleType,
            productImageContents,
            this.brand,
            this.name,
            this.price,
            this.modelNumber,
            this.category,
            this.releaseDate,
            this.description
        );
    }
}