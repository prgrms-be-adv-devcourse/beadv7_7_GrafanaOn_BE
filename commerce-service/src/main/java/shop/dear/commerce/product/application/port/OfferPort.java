package shop.dear.commerce.product.application.port;

import shop.dear.commerce.product.application.dto.external.ExistsOffer;

public interface OfferPort {
    ExistsOffer existsOffer(final Long productId);
}
