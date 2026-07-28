package shop.dear.identity.member.presentation.dto.response;

public record SellerCheckResponse(
    boolean isSeller
) {

    public static SellerCheckResponse from(boolean isSeller) {
        return new SellerCheckResponse(isSeller);
    }
}
