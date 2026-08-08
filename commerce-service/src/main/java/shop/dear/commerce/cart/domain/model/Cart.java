package shop.dear.commerce.cart.domain.model;

import jakarta.persistence.*;
import lombok.*;
import shop.dear.audit.BaseEntity;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table
@Getter
@Builder
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    public static Cart create(Long memberId) {
        return Cart.builder()
                .memberId(memberId)
                .build();
    }

}
