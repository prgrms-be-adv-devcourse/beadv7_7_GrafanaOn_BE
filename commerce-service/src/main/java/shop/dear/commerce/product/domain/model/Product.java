package shop.dear.commerce.product.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.commerce.product.domain.constant.ProductSaleType;
import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.commerce.product.domain.exception.ProductErrorCode;
import shop.dear.common.audit.BaseEntity;
import shop.dear.common.exception.BusinessException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static shop.dear.commerce.product.domain.exception.ProductErrorCode.EXCEEDED_BRAND_LENGTH_LIMIT;
import static shop.dear.commerce.product.domain.exception.ProductErrorCode.EXCEEDED_MODEL_NUMBER_LENGTH_LIMIT;
import static shop.dear.commerce.product.domain.exception.ProductErrorCode.INVALID_BRAND;
import static shop.dear.commerce.product.domain.exception.ProductErrorCode.INVALID_MODEL_NUMBER;
import static shop.dear.commerce.product.domain.exception.ProductErrorCode.INVALID_NAME;
import static shop.dear.commerce.product.domain.exception.ProductErrorCode.EXCEEDED_NAME_LENGTH_LIMIT;
import static shop.dear.commerce.product.domain.exception.ProductErrorCode.INVALID_RELEASE_DATE;
import static shop.dear.commerce.product.domain.exception.ProductErrorCode.NOT_PRODUCT_SELLER;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product")
@Entity
public class Product extends BaseEntity {

    public static final int MAX_NAME_LENGTH = 150;
    public static final int MAX_BRAND_LENGTH = 150;
    public static final int MAX_MODEL_NUMBER_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    public static final int PRODUCT_IMAGE_COUNT_LIMIT = 5;

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
    private LocalDate releaseDate;

    @Embedded
    private Price price;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false)
    private ProductSaleType saleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "description", length = 1000, nullable = true)
    private String description;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    public static Product create(
        final Long sellerId,
        final String name,
        final String brand,
        final String modelNumber,
        final ProductCategory category,
        final LocalDate releaseDate,
        final Price price,
        final ProductSaleType saleType,
        final String description
    ) {
        return new Product(
            sellerId,
            name,
            brand,
            modelNumber,
            category,
            releaseDate,
            price,
            saleType,
            description
        );
    }

    private Product(
        final Long sellerId,
        final String name,
        final String brand,
        final String modelNumber,
        final ProductCategory category,
        final LocalDate releaseDate,
        final Price price,
        final ProductSaleType saleType,
        final String description
    ) {
        this.sellerId = sellerId;
        this.name = validateName(name);
        this.brand = validateBrand(brand);
        this.modelNumber = validateModelNumber(modelNumber);
        this.category = validateCategory(category);
        this.releaseDate = validateReleaseDate(releaseDate);
        this.price = price;
        this.saleType = validateSaleType(saleType);
        this.status = ProductStatus.PREPARING;
        this.viewCount = 0L;
        this.description = validateDescription(description);
    }

    private String validateName(final String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(INVALID_NAME);
        }

        if (name.length() > MAX_NAME_LENGTH) {
            throw new BusinessException(EXCEEDED_NAME_LENGTH_LIMIT);
        }

        return name;
    }

    private String validateBrand(final String brand) {
        if (brand == null || brand.isBlank()) {
            throw new BusinessException(INVALID_BRAND);
        }

        if (brand.length() > MAX_BRAND_LENGTH) {
            throw new BusinessException(EXCEEDED_BRAND_LENGTH_LIMIT);
        }

        return brand;
    }

    private String validateModelNumber(final String modelNumber) {
        if (modelNumber == null || modelNumber.isBlank()) {
            throw new BusinessException(INVALID_MODEL_NUMBER);
        }

        if (modelNumber.length() > MAX_MODEL_NUMBER_LENGTH) {
            throw new BusinessException(EXCEEDED_MODEL_NUMBER_LENGTH_LIMIT);
        }

        return modelNumber;
    }

    private ProductCategory validateCategory(final ProductCategory category) {
        if (category == null) {
            throw new BusinessException(ProductErrorCode.INVALID_PRODUCT_CATEGORY);
        }

        return category;
    }

    private LocalDate validateReleaseDate(final LocalDate releaseDate) {
        if (releaseDate != null && releaseDate.isAfter(LocalDate.now())) {
            throw new BusinessException(INVALID_RELEASE_DATE);
        }

        return releaseDate;
    }

    private ProductSaleType validateSaleType(final ProductSaleType saleType) {
        if (saleType == null) {
            throw new BusinessException(ProductErrorCode.INVALID_PRODUCT_SALE_TYPE);
        }

        return saleType;
    }

    private String validateDescription(final String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException(ProductErrorCode.EXCEEDED_DESCRIPTION_LENGTH_LIMIT);
        }

        return description;
    }

    public Product update(
        final String name,
        final String brand,
        final String modelNumber,
        final ProductCategory category,
        final LocalDate releaseDate,
        final Price price,
        final String description
    ) {
        this.name = validateName(name);
        this.brand = validateBrand(brand);
        this.modelNumber = validateModelNumber(modelNumber);
        this.category = validateCategory(category);
        this.releaseDate = validateReleaseDate(releaseDate);
        this.price = price;
        this.description = validateDescription(description);

        this.images.clear();

        return this;
    }

    public ProductImage addImage(final String url, final int sortOrder) {
        validateImageCountLimit();

        final ProductImage image = ProductImage.create(this, url, sortOrder);
        this.images.add(image);

        return image;
    }

    private void validateImageCountLimit() {
        if (this.images.size() >= PRODUCT_IMAGE_COUNT_LIMIT) {
            throw new BusinessException(ProductErrorCode.EXCEEDED_PRODUCT_IMAGE_COUNT_LIMIT);
        }
    }

    public void validateOwner(final Long memberId) {
        if (!this.sellerId.equals(memberId)) {
            throw new BusinessException(NOT_PRODUCT_SELLER);
        }
    }

    public boolean isUpdatable() {
        return this.status == ProductStatus.PREPARING || this.status == ProductStatus.ON_SALE;
    }

    public void changeStatusToSoldOut() {
        this.status = ProductStatus.SOLD_OUT;
    }

    public void changeStatusToOnSale() {
        this.status = ProductStatus.ON_SALE;
    }
}
