package shop.dear.commerce.cart.domain.model;

import jakarta.persistence.*;
import lombok.*;
import shop.dear.commerce.cart.domain.exception.CartErrorCode;
import shop.dear.common.audit.BaseEntity;
import shop.dear.common.exception.BusinessException;

@Entity
@Table(name = "cart_item")
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

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public static CartItem create(Long cartId, Long proudctId) {
        return CartItem.builder()
                .cartId(cartId)
                .productId(proudctId)
                .build();
    }

    public void increaseQuantity(int amount) {
        this.quantity += amount;
    }

    public void chageQuantity(int quantity) {
        if(quantity <= 0) {
            throw new BusinessException(CartErrorCode.MIN_QUANTITY_REQUIRED);
        }
        this.quantity = quantity;
    }

}
