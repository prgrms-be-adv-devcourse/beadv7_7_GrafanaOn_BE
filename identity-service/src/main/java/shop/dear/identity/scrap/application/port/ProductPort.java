package shop.dear.identity.scrap.application.port;

import shop.dear.identity.scrap.application.dto.external.ProductSummary;

import java.util.List;

public interface ProductPort {

    List<ProductSummary> getProducts(List<Long> productIds);
}
