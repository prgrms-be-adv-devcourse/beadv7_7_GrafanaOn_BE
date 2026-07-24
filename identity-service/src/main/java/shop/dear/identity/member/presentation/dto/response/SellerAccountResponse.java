package shop.dear.identity.member.presentation.dto.response;

import shop.dear.identity.member.application.dto.SellerInfo;

public record SellerAccountResponse(
    String bank,
    String account
){

    public static SellerAccountResponse from(final SellerInfo info) {
        return new SellerAccountResponse(
            info.bank(),
            info.account()
        );
    }
}
