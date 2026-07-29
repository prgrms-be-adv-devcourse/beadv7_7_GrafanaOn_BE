package shop.dear.commerce.product.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import shop.dear.commerce.product.domain.exception.ProductImageErrorCode;
import shop.dear.common.audit.BaseEntity;
import shop.dear.common.exception.BusinessException;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product_image")
@Entity
public class ProductImage extends BaseEntity {

    public static final int MAX_URL_LENGTH = 500;
    public static final int MIN_SORT_ORDER = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "url", length = 500, nullable = false)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @SQLRestriction("deleted_at IS NULL")
    @OneToOne(mappedBy = "productImage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Story story;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    static ProductImage create(
        final Product product,
        final String url,
        final int sortOrder
    ) {
        return new ProductImage(
            product,
            url,
            sortOrder
        );
    }

    private ProductImage(
        final Product product,
        final String url,
        final int sortOrder
    ) {
        this.product = product;
        this.url = validateUrl(url);
        this.sortOrder = validateSortOrder(sortOrder);
    }

    private String validateUrl(final String url) {
        if (url == null || url.isBlank()) {
            throw new BusinessException(ProductImageErrorCode.INVALID_URL);
        }

        if (url.length() > MAX_URL_LENGTH) {
            throw new BusinessException(ProductImageErrorCode.EXCEEDED_URL_LENGTH_LIMIT);
        }

        return url;
    }

    private int validateSortOrder(final int sortOrder) {
        if (sortOrder < MIN_SORT_ORDER || sortOrder > Product.PRODUCT_IMAGE_COUNT_LIMIT) {
            throw new BusinessException(ProductImageErrorCode.INVALID_SORT_ORDER);
        }

        return sortOrder;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();

        if (this.story != null) {
            this.story.delete();
        }
    }

    public void addStory(final String content) {
        this.story = Story.create(this, content);
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
