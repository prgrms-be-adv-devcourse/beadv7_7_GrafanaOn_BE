package shop.dear.commerce.product.application.fake;

import shop.dear.commerce.product.application.dto.external.ExistsOffer;
import shop.dear.commerce.product.application.port.OfferPort;

public class FakeOfferPort implements OfferPort {

    @Override
    public ExistsOffer existsOffer(final Long productId) {
        return new ExistsOffer(true);
    }
}
