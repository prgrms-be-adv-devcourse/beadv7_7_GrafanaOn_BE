package shop.dear.identity.scrap.presentation.dto;

import shop.dear.identity.scrap.application.dto.ScrapInfo;

public record ScrapResponse(Long id, Long productId) {

    public static ScrapResponse from(final ScrapInfo info) {
        return new ScrapResponse(info.id(), info.productId());
    }
}
