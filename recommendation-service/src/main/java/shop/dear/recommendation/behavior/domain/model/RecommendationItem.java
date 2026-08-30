package shop.dear.recommendation.behavior.domain.model;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.audit.BaseEntity;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.commerce.product.domain.constant.ProductStatus;

@Getter
@Entity
@Table(
        name = "recommendation_item",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recommendation_item_product_id",
                        columnNames = "product_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    private RecommendationItem(
            final Long productId,
            final ProductCategory category,
            final ProductStatus status,
            final Long viewCount
    ){
        this.productId = productId;
        this.category = category;
        this.status = status;
        this.viewCount = viewCount;
    }

    public static RecommendationItem create(
            final Long productId,
            final ProductCategory category,
            final ProductStatus status,
            final Long viewCount
    ){
        return new RecommendationItem(productId, category, status, viewCount);
    }

}
