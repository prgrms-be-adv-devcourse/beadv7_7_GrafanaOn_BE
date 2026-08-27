package shop.dear.common.messaging.consumer;

import com.rabbitmq.stream.Consumer;
import com.rabbitmq.stream.Message;
import com.rabbitmq.stream.MessageHandler;
import com.rabbitmq.stream.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shop.dear.common.messaging.StreamMessageHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("RabbitMQ Stream 메시지 Listener")
class RabbitStreamMessageListenerTest {

    @Test
    @DisplayName("메시지 처리가 성공하면 현재 오프셋을 저장한다")
    void onStreamMessage_handlerSuccess_storesOffset() {
        StreamMessageHandler messageHandler = mock(StreamMessageHandler.class);
        StreamConsumerFailureHandler failureHandler = mock(StreamConsumerFailureHandler.class);
        RabbitStreamMessageListener listener =
                new RabbitStreamMessageListener(messageHandler, failureHandler);

        MessageHandler.Context context = mock(MessageHandler.Context.class);
        when(context.offset()).thenReturn(10L);

        listener.onStreamMessage(testMessage(), context);

        verify(messageHandler).handle(new StreamMessage(
                "event-1",
                "PAYMENT_REQUESTED",
                "{\"paymentId\":1}",
                10L
        ));
        verify(context).storeOffset();
        verifyNoInteractions(failureHandler);
    }

    @Test
    @DisplayName("메시지 변환에 실패하면 offset을 저장하지 않고 Consumer를 종료한 뒤 재시작을 요청한다")
    void onStreamMessage_messageConversionFailure_closesConsumerAndRequestsRestart() {
        StreamMessageHandler messageHandler = mock(StreamMessageHandler.class);
        StreamConsumerFailureHandler failureHandler = mock(StreamConsumerFailureHandler.class);
        RabbitStreamMessageListener listener =
                new RabbitStreamMessageListener(messageHandler, failureHandler);

        RuntimeException exception = new RuntimeException("메시지 변환 실패");
        Message message = mock(Message.class);
        when(message.getProperties()).thenThrow(exception);

        MessageHandler.Context context = mock(MessageHandler.Context.class);
        Consumer consumer = mock(Consumer.class);
        when(context.consumer()).thenReturn(consumer);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> listener.onStreamMessage(message, context)
        );

        assertEquals(exception, thrown);
        verifyNoInteractions(messageHandler);
        verify(context, never()).storeOffset();
        verify(consumer).close();
        verify(failureHandler).onFailure(exception);
    }

    @Test
    @DisplayName("메시지 처리에 실패하면 native Consumer를 종료하고 재시작을 요청한다")
    void onStreamMessage_handlerFailure_closesConsumerAndRequestsRestart() {
        StreamMessageHandler messageHandler = mock(StreamMessageHandler.class);
        StreamConsumerFailureHandler failureHandler = mock(StreamConsumerFailureHandler.class);
        RabbitStreamMessageListener listener =
                new RabbitStreamMessageListener(messageHandler, failureHandler);

        RuntimeException exception = new RuntimeException("처리 실패");
        doThrow(exception).when(messageHandler).handle(any(StreamMessage.class));

        MessageHandler.Context context = mock(MessageHandler.Context.class);
        Consumer consumer = mock(Consumer.class);
        when(context.offset()).thenReturn(10L);
        when(context.consumer()).thenReturn(consumer);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> listener.onStreamMessage(testMessage(), context)
        );

        assertEquals(exception, thrown);
        verify(context, never()).storeOffset();
        verify(consumer).close();
        verify(failureHandler).onFailure(exception);
    }

    private Message testMessage() {
        Message message = mock(Message.class);
        Properties properties = mock(Properties.class);

        when(properties.getMessageIdAsString()).thenReturn("event-1");
        when(message.getProperties()).thenReturn(properties);
        when(message.getApplicationProperties()).thenReturn(
                Map.of(StreamMessageHeaders.EVENT_TYPE, "PAYMENT_REQUESTED")
        );
        when(message.getBodyAsBinary()).thenReturn(
                "{\"paymentId\":1}".getBytes(StandardCharsets.UTF_8)
        );

        return message;
    }
}