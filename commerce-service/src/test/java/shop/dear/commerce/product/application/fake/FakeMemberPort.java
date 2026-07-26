package shop.dear.commerce.product.application.fake;

import lombok.extern.slf4j.Slf4j;
import shop.dear.commerce.product.application.dto.external.IsSeller;
import shop.dear.commerce.product.application.dto.external.MemberProfile;
import shop.dear.commerce.product.application.port.MemberPort;

@Slf4j
public class FakeMemberPort implements MemberPort {

    @Override
    public MemberProfile getMemberProfile(final Long memberId) {
        log.info("[FakeMemberPort] get member profile: {}", memberId);

        return new MemberProfile(
            "name",
            "nickname",
            "email",
            "address",
            "number"
        );
    }

    @Override
    public IsSeller isSeller(final Long memberId) {
        return new IsSeller(true);
    }
}
