package shop.dear.common.messaging.consumer;

/**
 * 각 BC가 구현하는 Stream 메시지 처리 진입점입니다.
 *
 * <p>메서드가 정상 반환되면 공통 Listener는 해당 메시지 처리가 확정됐다고 보고 offset을 저장합니다.
 * DB 변경이나 실패 기록을 확정하지 못했다면 RuntimeException을 던져 offset 저장과 다음 메시지 처리를 막아야 합니다.</p>
 */
@FunctionalInterface
public interface StreamMessageHandler {

    /**
     * 수신한 공통 메시지를 해당 BC의 도메인 로직으로 처리합니다.
     *
     * RabbitMQ native 메시지를 공통 형식으로 변환한 메시지
     * @param message
     */
    void handle(StreamMessage message);
}
