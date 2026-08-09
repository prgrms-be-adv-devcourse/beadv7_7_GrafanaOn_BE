package shop.dear.commerce.product.application.port;

import shop.dear.commerce.product.application.dto.external.ExistsMember;
import shop.dear.commerce.product.application.dto.external.IsSeller;
import shop.dear.commerce.product.application.dto.external.MemberProfile;

public interface MemberPort {
    ExistsMember existsMember(final Long memberId);
    MemberProfile getMemberProfile(final Long memberId);
    IsSeller isSeller(final Long memberId);
}
