package shop.dear.identity.member.application.dto;

import shop.dear.identity.member.domain.model.Seller;

public record SellerInfo(
    String bank,
    String account
){

    public static SellerInfo from(final Seller seller) {
        return new SellerInfo(
            seller.getBank(),
            seller.getAccount()
        );
    }
}
