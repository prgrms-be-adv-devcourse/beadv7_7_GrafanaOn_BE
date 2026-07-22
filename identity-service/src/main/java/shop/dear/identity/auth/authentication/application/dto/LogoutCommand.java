package shop.dear.identity.auth.authentication.application.dto;

// 로그아웃 DTO
public record LogoutCommand(
        Long memberId // 출처는 AccessToken
        // 로그아웃은 Refresh Token이 만료됐더라도 수행할 수 있어야 한다.
) {
}
