package shop.dear.gateway.fallback;

/**
 * 하위 서비스의 ApiResponse와 같은 모양을 유지한다.
 * common-web을 직접 의존하지 않는다. 게이트웨이에서 따로 정의한다.
 */
public record FallbackResponse(
        String code,
        String message
) {
}
