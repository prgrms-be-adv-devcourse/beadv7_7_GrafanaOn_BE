package shop.deal.commerce.search.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import shop.deal.commerce.search.domain.SearchProduct;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "search_product")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchProductJpaEntity {

    @Id // 이 id는 Product가 보낸 id다. 따라서 직접 id를 생성하지 않는다.
    @Column(name = "id")
    private Long productId;

    @Column(name = "name", nullable = false, length = 150)
    private String productName;

    @Column(name = "model_number", nullable = false, length = 100)
    private String modelNumber;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal productPrice;

    @Column(name = "sale_type", nullable = false, length = 30)
    private String saleType;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "inserted_at", nullable = false)
    private LocalDateTime insertedAt;

    public static SearchProductJpaEntity from(SearchProduct product) {
        return SearchProductJpaEntity.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .modelNumber(product.getModelNumber())
                .category(product.getCategory())
                .releaseDate(product.getReleaseDate())
                .productPrice(product.getProductPrice())
                .saleType(product.getSaleType())
                .viewCount(product.getViewCount())
                .description(product.getDescription())
                .insertedAt(product.getInsertedAt())
                .build();
    }

    public SearchProduct toDomain() {
        return new SearchProduct(
                productId,
                productName,
                modelNumber,
                category,
                releaseDate,
                productPrice,
                saleType,
                viewCount,
                description,
                insertedAt
        );
    }
}