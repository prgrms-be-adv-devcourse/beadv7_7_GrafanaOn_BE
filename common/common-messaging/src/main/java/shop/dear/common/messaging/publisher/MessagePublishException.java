package shop.dear.common.messaging.publisher;

/**
 * RabbitMQ Stream 발행 결과가 실패로 확인된 경우 발생하는 예외
 */
public class MessagePublishException extends RuntimeException {

    public MessagePublishException(String message) {
        super(message);
    }
}
