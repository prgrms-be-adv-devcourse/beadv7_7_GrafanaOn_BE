package shop.deal.commerce.product.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.deal.commerce.product.domain.constant.ProductCategory;
import shop.deal.commerce.product.domain.constant.ProductSaleType;
import shop.deal.commerce.product.domain.constant.ProductStatus;
import shop.deal.common.audit.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product")
@Entity
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "brand", length = 150, nullable = false)
    private String brand;

    @Column(name = "model_number", length = 100, nullable = false)
    private String modelNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ProductCategory category;

    @Column(name = "release_date", nullable = true)
    private Date releaseDate;

    @Column(name = "price", precision = 15, scale = 2, nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false)
    private ProductSaleType saleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    private String description;
}
