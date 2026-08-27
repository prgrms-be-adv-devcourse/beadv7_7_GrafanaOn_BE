package shop.dear.common.messaging.consumer;

/**
 * 메시지 처리를 확정하지 못한 예외를 Consumer 인프라에 전달하는 콜백입니다.
 *
 * <p>Listener는 실패 시 native Consumer를 먼저 닫아 실패 offset이 건너뛰어지는 것을 막고,
 * 그 다음 이 콜백을 호출합니다.
 * Container 재시작 예약은 Factory가 수행합니다.</p>
 */
@FunctionalInterface
public interface StreamConsumerFailureHandler {

    /**
     * 처리 확정에 실패한 예외를 전달합니다.
     *
     * @param cause 메시지 처리 중 발생한 예외
     */
    void onFailure(Throwable cause);
}
