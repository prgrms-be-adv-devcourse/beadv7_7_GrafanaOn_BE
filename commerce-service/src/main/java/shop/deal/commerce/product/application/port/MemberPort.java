package shop.deal.commerce.product.application.port;

import shop.deal.commerce.product.application.dto.external.MemberProfile;

public interface MemberPort {

    MemberProfile getMemberProfile(final Long memberId);
}
