package shop.dear.identity.auth.authentication.application.port;

import shop.dear.identity.auth.authentication.application.dto.MemberProfileResult;

// 이메일, 비번 없음.
public interface MemberProfilePort {
    MemberProfileResult createProfile(
            String name,
            String defaultShippingAddress,
            String phoneNumber
    );

    void withdrawProfile(Long memberId); // Member 프로필의 개인정보 처리 요청
}
