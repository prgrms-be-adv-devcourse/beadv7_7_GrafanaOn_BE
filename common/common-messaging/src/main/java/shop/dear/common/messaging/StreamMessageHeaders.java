package shop.dear.common.messaging;

/**
 * Publisher와 Consumer가 공통으로 사용하는 Stream 메시지 header 이름을 정의합니다.
 */
public final class StreamMessageHeaders {

    /**
     * Consumer가 메시지 유형을 식별할 때 사용하는 eventType header 이름입니다.
     */
    public static final String EVENT_TYPE = "eventType";

    private StreamMessageHeaders() {
    }
}
