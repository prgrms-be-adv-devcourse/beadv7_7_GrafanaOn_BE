package shop.dear.common.messaging.publisher;

import java.util.concurrent.CompletableFuture;

public interface StreamPublisher {

    /**
     * JSON payload를 지정한 Stream으로 비동기 발행합니다.
     *
     * 반환된 Future는 발행 결과가 확인된 후 완료됩니다.
     * 발행에 성공하면 반환값 없이 정상 완료되고,
     * 연결·통신 오류 또는 발행 실패 시 예외 완료됩니다.
     *
     * @param streamName 발행할 Stream 이름
     * @param eventId 메시지 식별자
     * @param eventType 이벤트 유형
     * @param payload JSON 형식의 메시지 본문
     * @return 발행 결과를 나타내는 Future
     */
    CompletableFuture<Void> publish(
            String streamName,
            String eventId,
            String eventType,
            String payload
    );
}
