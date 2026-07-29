package shop.dear.gateway.auth;

public record AuthenticatedUser (
        Long memberId,
        // String으로 받는다. identity 모듈 의존 X
        String role
) {
}
