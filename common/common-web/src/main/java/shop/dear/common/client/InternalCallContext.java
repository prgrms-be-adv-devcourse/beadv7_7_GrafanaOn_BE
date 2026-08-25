package shop.dear.common.client;

/**
 * 서비스 간 호출에 실어 보낼 인증 정보를 담는다.
 *
 * 차단기가 실제 호출을 별도 스레드에서 실행하기 때문에, 요청 스레드의
 * ServletRequestAttributes를 그대로 넘기면 HttpServletRequest가 스레드를 건너간다.
 * 서블릿 컨테이너는 request 객체의 스레드 안전성을 보장하지 않고 응답 후 재활용하니 객체 대신 필요한 값만 문자열로 떠넘긴다.
 */
public class InternalCallContext {

    private static final ThreadLocal<String> MEMBER_ID = new ThreadLocal<>();

    private InternalCallContext() {}

    public static void setMemberId(final String memberId) {
        MEMBER_ID.set(memberId);
    }

    public static String getMemberId() {
        return MEMBER_ID.get();
    }

    public static void clear() {
        MEMBER_ID.remove();
    }
}
