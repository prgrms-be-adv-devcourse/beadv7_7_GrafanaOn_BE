package shop.dear.common.messaging.consumer;

import com.rabbitmq.stream.Message;
import com.rabbitmq.stream.MessageHandler;
import org.springframework.rabbit.stream.listener.StreamMessageListener;
import shop.dear.common.messaging.StreamMessageHeaders;

import java.nio.charset.StandardCharsets;

/**
 * RabbitMQ Stream native 메시지를 공통 {@link StreamMessage}로 변환하는 Listener입니다.
 *
 * <p>도메인 handler가 정상 종료하면 현재 offset을 저장합니다.
 * handler가 예외를 던지면 offset을 저장하지 않고 native Consumer를 닫은 뒤,
 * Factory에 Container 재시작을 요청합니다.</p>
 */
public class RabbitStreamMessageListener implements StreamMessageListener {

    // 각 BC의 실제 도메인 메시지 처리를 수행하는 콜백
    private final StreamMessageHandler messageHandler;

    // 처리 확정 실패 시 Factory에 Consumer 재시작을 요청하는 콜백
    private final StreamConsumerFailureHandler failureHandler;

    public RabbitStreamMessageListener(
            final StreamMessageHandler messageHandler,
            final StreamConsumerFailureHandler failureHandler
    ) {
        this.messageHandler = messageHandler;
        this.failureHandler = failureHandler;
    }

    @Override
    public void onStreamMessage(
            Message message,
            MessageHandler.Context context) {
        // RabbitMQ native 메시지의 metadata와 body를 BC가 공통으로 쓰는 StreamMessage로 변환
        StreamMessage streamMessage = new StreamMessage(
                // Publisher가 messageId에 넣은 이벤트 식별자
                message.getProperties().getMessageIdAsString(),

                // Publisher가 custom header에 넣은 이벤트 유형
                (String) message.getApplicationProperties()
                        .get(StreamMessageHeaders.EVENT_TYPE),

                // byte[] 형태의 JSON body를 UTF-8 문자열로 변환
                new String(message.getBodyAsBinary(), StandardCharsets.UTF_8),

                // 현재 메시지가 Stream 안에서 가진 고정 위치
                // 실제 offset 저장 여부는 handler 성공 뒤 context.storeOffset()에서 결정
                context.offset());

        // 변환한 메시지를 도메인 측 Handler에 전달
        try {
            messageHandler.handle(streamMessage);

            // Handler가 예외 없이 완료된 경우에만 현재 메시지 offset 저장
            context.storeOffset();
        } catch (RuntimeException exception) {
            // broker와 실제 통신 중인 native Consumer를 즉시 닫아,
            // 후속 메시지의 offset 저장이 현재 실패 메시지를 건너뛰는 것을 막는다.
            context.consumer().close();

            // Spring Container의 재시작을 담당하는 Factory에 실패를 알린다.
            failureHandler.onFailure(exception);

            throw exception;
        }
    }

    // MessageListener 상속 계약을 만족하기 위해 구현
    // StreamListenerContainer는 native 메시지 수신 시 onStreamMessage()를 호출
    @Override
    public void onMessage(org.springframework.amqp.core.Message message) {
        throw new UnsupportedOperationException(
                "RabbitMQ Stream native 메시지만 처리할 수 있습니다.");
    }
}
