package shop.dear.commerce.cart.application.port;


import shop.dear.commerce.cart.application.port.dto.CartProductInfo;

import java.util.List;

public interface CartProductPort {
    CartProductInfo getProduct(Long productId);
    List<CartProductInfo>  getProducts(List<Long> productIds);
}
