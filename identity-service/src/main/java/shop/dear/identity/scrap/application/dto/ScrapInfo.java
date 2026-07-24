package shop.dear.identity.scrap.application.dto;

import shop.dear.identity.scrap.domain.model.Scrap;

public record ScrapInfo(Long id, Long memberId, Long productId) {

    public static ScrapInfo from(final Scrap scrap) {
        return new ScrapInfo(
            scrap.getId(),
            scrap.getMemberId(),
            scrap.getProductId()
        );
    }
}
