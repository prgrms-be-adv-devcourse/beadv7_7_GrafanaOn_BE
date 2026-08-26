package shop.dear.common.messaging.publisher;

import com.rabbitmq.stream.Environment;
import jakarta.annotation.PreDestroy;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON payload를 RabbitMQ Stream으로 발행하는 StreamPublisher 구현체
 */
@Component
public class RabbitStreamPublisher implements StreamPublisher {

    private static final String EVENT_TYPE_HEADER = "eventType";

    private final Environment environment;

    // Stream별 Template을 재사용하기 위한 인메모리 캐시
    private final Map<String, RabbitStreamTemplate> templates = new ConcurrentHashMap<>();

    public RabbitStreamPublisher(Environment environment) {
        this.environment = environment;
    }

    @Override
    public CompletableFuture<Void> publish(
            String streamName,
            String eventId,
            String eventType,
            String payload
    ) {
        // 대상 Stream에 맞는 Template을 조회하고, 없으면 생성해 캐시에 저장
        RabbitStreamTemplate template = templates.computeIfAbsent(
                streamName,
                name -> new RabbitStreamTemplate(environment, name)
        );

        // eventId, eventType, 본문 형식 등 메시지 메타데이터 구성
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(eventId);
        properties.setHeader(EVENT_TYPE_HEADER, eventType);
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());

        // JSON 문자열을 UTF-8 바이트 배열로 변환해 Rabbit 메시지 생성
        Message message = new Message(
                payload.getBytes(StandardCharsets.UTF_8),
                properties);

        // RabbitMQ Stream에 메시지 전송을 요청하고, 발행 결과 Future 객체를 반환받습니다.
        CompletableFuture<Boolean> sendResult = template.send(message);

        // RabbitMQ Stream의 Boolean 발행 결과를 StreamPublisher의 Future<Void> 계약으로 변환
        return sendResult.thenApply(published -> {
            if (published) {
                // 발행 성공 시 반환값 없이 정상 완료
                return null;
            }

            // Broker가 발행 실패 결과를 반환하면 예외 완료
            throw new MessagePublishException(
                    "RabbitMQ Stream 메시지 발행이 실패했습니다."
            );
        });
    }

    @PreDestroy
    public void close() {
        // 애플리케이션 종료 시 생성·캐시한 Template을 모두 정리
        templates.values().forEach(RabbitStreamTemplate::close);
    }
}
