package shop.dear.identity.auth.authentication.application.dto;

public enum RefreshTokenVerificationResult {
    MATCHED,// 정상. 현재 저장된 Refresh Token
    NOT_FOUND, // 로그아웃 등으로 저장된 세션 없음
    EXPIRED,// DB에 저장된 Token 만료
    MISMATCHED // 현재 토큰이 따로 이쓴데 다른 유효한 Refresh Token이 들어옴 => 실제 재사용으로 잡을 것.
}
