package shop.dear.identity.scrap.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.common.audit.BaseEntity;

@Entity
@Table(name = "scrap")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scrap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    private Scrap(final Long memberId, final Long productId
    ){
        this.memberId = memberId;
        this.productId = productId;
    }

    public static Scrap create(final Long memberId, final Long productId
    ){
        return new Scrap(memberId, productId);
    }
}
