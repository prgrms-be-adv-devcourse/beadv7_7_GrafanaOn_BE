package shop.dear.recommendation.behavior.application.port;

import shop.dear.commerce.product.domain.constant.ProductCategory;

import java.util.Map;

public interface ProductPort {

    Map<Long, ProductCategory> getProductCategories(java.util.List<Long> productIds);
}
