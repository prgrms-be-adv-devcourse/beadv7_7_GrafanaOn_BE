package shop.dear.commerce.product.presentation.dto.response;

import shop.dear.commerce.product.application.dto.MemberProductExistsDto;

public record GetMemberProductExistsResponse(
    boolean exists
) {
    public static GetMemberProductExistsResponse of(final MemberProductExistsDto dto) {
        return new GetMemberProductExistsResponse(dto.exists());
    }
}
