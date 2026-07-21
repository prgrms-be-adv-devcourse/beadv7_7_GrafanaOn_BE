package shop.deal.commerce.order.offer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.deal.common.audit.BaseEntity;
import shop.deal.common.exception.BusinessException;
import shop.deal.commerce.order.offer.domain.exception.OfferErrorCode;

import java.math.BigDecimal;

/**
 * 오퍼 작성 시작 시점의 상품 정보(모델 번호, 가격)를 보존하는 스냅샷 엔티티
 * 오퍼 생성 전까지는 독립적으로 존재하다가 오퍼 제출 시점에 offerId로 연결됨
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfferSnapshot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offer_id")
    private Long offerId;

    @Column(name = "writer_id", nullable = false)
    private Long writerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "model_snapshot", nullable = false, length = 100)
    private String modelSnapshot;

    @Column(name = "price_snapshot", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceSnapshot;

    private OfferSnapshot(
        final Long writerId,
        final Long productId,
        final String modelSnapshot,
        final BigDecimal priceSnapshot
    ) {
        this.writerId = writerId;
        this.productId = productId;
        this.modelSnapshot = modelSnapshot;
        this.priceSnapshot = priceSnapshot;
    }

    public static OfferSnapshot create(
        final Long writerId,
        final Long productId,
        final String modelSnapshot,
        final BigDecimal priceSnapshot
    ) {
        return new OfferSnapshot(writerId, productId, modelSnapshot, priceSnapshot);
    }

    public void linkToOffer(final Long offerId) {
        if (this.offerId != null) {
            throw new BusinessException(OfferErrorCode.OFFER_SNAPSHOT_ALREADY_LINKED);
        }
        this.offerId = offerId;
    }
}
