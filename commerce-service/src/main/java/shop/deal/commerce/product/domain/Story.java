package shop.deal.commerce.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.deal.common.audit.BaseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "story")
@Entity
public class Story extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_image_id")
    private ProductImage productImage;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
}
