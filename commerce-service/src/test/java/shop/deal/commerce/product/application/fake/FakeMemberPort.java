package shop.deal.commerce.product.application.fake;

import shop.deal.commerce.product.application.dto.external.MemberProfile;
import shop.deal.commerce.product.application.port.MemberPort;

public class FakeMemberPort implements MemberPort {

    @Override
    public MemberProfile getMemberProfile(final Long memberId) {
        return new MemberProfile(
            "name",
            "nickname",
            "email",
            "address",
            "number"
        );
    }
}
