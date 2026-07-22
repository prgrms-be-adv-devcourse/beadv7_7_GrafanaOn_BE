package shop.dear.identity.auth.authentication.application.dto;

// 회원가입 결과 DTO
public record SignUpResult(
        Long memberId,
        String email,
        String nickname // Member 프로필 생성 결과로 받는다.
) {
}
