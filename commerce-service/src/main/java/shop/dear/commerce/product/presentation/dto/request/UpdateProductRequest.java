package shop.dear.commerce.product.presentation.dto.request;

import shop.dear.commerce.product.application.dto.command.UpdateProductCommand;
import shop.dear.commerce.product.domain.constant.ProductCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateProductRequest(
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

    public UpdateProductCommand toCommand() {
        final List<UpdateProductCommand.ProductImageContentCommand> productImageContents = this.productImageContents.stream()
            .map(img -> new UpdateProductCommand.ProductImageContentCommand(
                img.sortOrder,
                img.url,
                img.story
            ))
            .toList();

        return new UpdateProductCommand(
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