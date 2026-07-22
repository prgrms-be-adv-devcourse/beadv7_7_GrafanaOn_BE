package shop.deal.identity.auth.authentication.application;

import shop.deal.identity.auth.authentication.application.dto.MemberProfileResult;

// 이메일, 비번 없음.
public interface MemberProfilePort {
    MemberProfileResult createProfile(
            String name,
            String defaultShippingAddress,
            String phoneNumber
    );
}
