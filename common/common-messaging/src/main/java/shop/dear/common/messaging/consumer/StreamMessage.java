package shop.dear.common.messaging.consumer;

/**
 * Stream으로부터 수신한 범용 메시지입니다.
 *
 * @param eventId 메시지 식별자
 * @param eventType 이벤트 유형
 * @param payload JSON 형식의 메시지 본문
 * @param offset Stream 내 메시지 위치
 */
public record StreamMessage(
        String eventId,
        String eventType,
        String payload,
        long offset
) {
}
