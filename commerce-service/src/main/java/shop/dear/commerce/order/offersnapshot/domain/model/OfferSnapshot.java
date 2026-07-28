package shop.dear.commerce.order.offersnapshot.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.common.audit.BaseEntity;
import shop.dear.common.exception.BusinessException;
import shop.dear.commerce.order.offersnapshot.domain.exception.OfferSnapshotErrorCode;

import java.math.BigDecimal;

/**
 * 오퍼 작성 시작 시점의 상품 정보(모델 번호, 가격)를 보존하는 스냅샷 엔티티
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "offer_snapshot")
public class OfferSnapshot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offer_id")
    private Long offerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "writer_id", nullable = false)
    private Long writerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "model_number_snapshot", nullable = false, length = 100)
    private String modelNumberSnapshot;

    @Column(name = "price_snapshot", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceSnapshot;

    private OfferSnapshot(
        final Long sellerId,
        final Long writerId,
        final Long productId,
        final String modelNumberSnapshot,
        final BigDecimal priceSnapshot
    ) {
        this.sellerId = sellerId;
        this.writerId = writerId;
        this.productId = productId;
        this.modelNumberSnapshot = modelNumberSnapshot;
        this.priceSnapshot = priceSnapshot;
    }

    public static OfferSnapshot create(
        final Long sellerId,
        final Long writerId,
        final Long productId,
        final String modelNumberSnapshot,
        final BigDecimal priceSnapshot
    ) {
        return new OfferSnapshot(sellerId, writerId, productId, modelNumberSnapshot, priceSnapshot);
    }

    public void updateSnapshot(
            final String modelNumberSnapshot,
            final BigDecimal priceSnapshot
    ) {
        this.modelNumberSnapshot = modelNumberSnapshot;
        this.priceSnapshot = priceSnapshot;
    }

    public void linkToOffer(final Long offerId, final Long writerId) {
        validateWriter(writerId);
        if (this.offerId != null) {
            throw new BusinessException(OfferSnapshotErrorCode.OFFER_SNAPSHOT_ALREADY_LINKED);
        }
        this.offerId = offerId;
    }

    private void validateWriter(final Long writerId) {
        if (!this.writerId.equals(writerId)) {
            throw new BusinessException(OfferSnapshotErrorCode.OFFER_SNAPSHOT_WRITER_MISMATCH);
        }
    }
}
