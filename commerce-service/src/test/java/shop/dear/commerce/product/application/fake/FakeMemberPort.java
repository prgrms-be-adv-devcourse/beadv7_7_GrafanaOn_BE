package shop.dear.commerce.product.application.fake;

import shop.dear.commerce.product.application.dto.external.MemberProfile;
import shop.dear.commerce.product.application.port.MemberPort;

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
