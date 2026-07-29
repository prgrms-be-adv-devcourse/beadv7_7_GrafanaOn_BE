package shop.dear.commerce.cart.domain.model;

import jakarta.persistence.*;
import lombok.*;
import shop.dear.commerce.cart.domain.constant.CartItemStatus;
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

    @Builder.Default
    @Column(name = "quantity", nullable = false)
    private int quantity = 0;

    @Builder.Default
    @Column(name = "cart_item_status", nullable = false)
    private CartItemStatus status = CartItemStatus.BEFORE_PAYMENT;

    public static CartItem create(Long cartId, Long proudctId, int quantity) {
        return CartItem.builder()
                .cartId(cartId)
                .productId(proudctId)
                .quantity(quantity)
                .build();
    }

    public void increaseQuantity(int amount) {
        this.quantity += amount;
    }

    public void markPaymentCompleted() {
        this.status = CartItemStatus.PAYMENT_COMPLETED;
    }

    public void markPaymentBefore() {
        this.status = CartItemStatus.BEFORE_PAYMENT;
    }

    public void chageQuantity(int quantity) {
        if(quantity <= 0) {
            throw new BusinessException(CartErrorCode.MIN_QUANTITY_REQUIRED);
        }
        this.quantity = quantity;
    }

}
