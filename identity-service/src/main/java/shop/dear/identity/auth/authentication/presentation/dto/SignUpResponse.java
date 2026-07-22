package shop.dear.identity.auth.authentication.presentation.dto;

import shop.dear.identity.auth.authentication.application.dto.SignUpResult;

// 회원가입 응답 DTO
public record SignUpResponse(
        Long memberId,
        String email,
        String nickname
) {
    public static SignUpResponse from(SignUpResult result) {
        return new SignUpResponse(
                result.memberId(),
                result.email(),
                result.nickname()
        );
    }
}
