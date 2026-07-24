package shop.dear.identity.auth.authentication.domain;

// 계정 상태
public enum AuthAccountStatus {
    PENDING, // Member 프로필 연결 전
    ACTIVE, // 회원가입 완료, 로그인 가능
    WITHDRAWN // 탈퇴한 이용자
}
