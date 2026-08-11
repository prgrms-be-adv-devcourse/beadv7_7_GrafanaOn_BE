package shop.dear.commerce.product.presentation.dto.response;

import shop.dear.commerce.product.application.dto.TradeProductDto;

public record TradeProductResponse(
    boolean isChanged
) {

    public static TradeProductResponse of(final TradeProductDto dto) {
        return new TradeProductResponse(dto.isChanged());
    }
}
