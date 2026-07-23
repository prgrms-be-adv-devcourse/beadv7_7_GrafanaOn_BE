package shop.dear.commerce.product.application.port;

import shop.dear.commerce.product.application.dto.external.IsSeller;
import shop.dear.commerce.product.application.dto.external.MemberProfile;

public interface MemberPort {

    MemberProfile getMemberProfile(final Long memberId);
    IsSeller isSeller(final Long memberId);
}
