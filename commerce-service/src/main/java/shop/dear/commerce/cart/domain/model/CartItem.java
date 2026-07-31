package shop.dear.commerce.cart.domain.model;

import jakarta.persistence.*;
import lombok.*;
import shop.dear.commerce.cart.domain.constant.CartItemStatus;
import shop.dear.commerce.cart.domain.exception.CartErrorCode;
import shop.dear.common.audit.BaseEntity;
import shop.dear.common.exception.BusinessException;

@Entity
@Table(
        name = "cart_item",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cart_item_cart_product",
                        columnNames = {"cart_id", "product_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Builder.Default
    @Column(name = "cart_item_status", nullable = false)
    private CartItemStatus status = CartItemStatus.BEFORE_PAYMENT;

    public static CartItem create(Long cartId, Long productId) {
        return CartItem.builder()
                .cartId(cartId)
                .productId(productId)
                .build();
    }

}
