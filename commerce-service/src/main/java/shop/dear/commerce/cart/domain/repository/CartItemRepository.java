package shop.dear.commerce.cart.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.cart.domain.model.CartItem;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);
    List<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    void deleteByCartIdAndProductId(Long cartId, Long productId);
    void deleteByCartIdAndProductIdIn(Long cartId, List<Long> productIds);
    void deleteByCartId(Long cartId);
}
