package shop.dear.common.messaging.serializer;

/**
 * 메시지 직렬화/역직렬화 과정에서 발생한 예외를 메시징 계층의 예외로 변환하기 위한 예외 클래스.
 * 원본 예외를 cause로 보존하여 실제 실패 원인을 추적할 수 있도록 함.
 */
public class MessageSerializationException extends RuntimeException {

    public MessageSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
