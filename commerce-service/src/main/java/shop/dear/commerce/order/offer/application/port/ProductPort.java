package shop.dear.commerce.order.offer.application.port;

import shop.dear.commerce.order.offer.application.port.dto.ProductInfo;

public interface ProductPort {

    ProductInfo getProduct(Long productId);
}
