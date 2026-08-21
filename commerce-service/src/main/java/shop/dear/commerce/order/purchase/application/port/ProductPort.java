package shop.dear.commerce.order.purchase.application.port;

import shop.dear.commerce.order.purchase.application.port.dto.ProductInfo;

public interface ProductPort {

  ProductInfo getProduct(Long productId);

  boolean tradeProduct(Long productId);
}
