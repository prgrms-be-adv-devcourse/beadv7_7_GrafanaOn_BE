package shop.dear.common.messaging.consumer;

import com.rabbitmq.stream.Environment;
import jakarta.annotation.PreDestroy;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;
import org.springframework.stereotype.Component;
import shop.dear.common.messaging.config.RabbitStreamProperties;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RabbitMQ Stream Consumer Container를 생성하고, 처리 실패 후 재시작을 관리하는 Factory
 *
 * <p>각 Consumer는 수동 offset 추적을 사용합니다.
 * 처리 실패 시 Listener가 native Consumer를 닫고,
 * Factory는 설정된 시간 뒤 Container를 재시작합니다.</p>
 */
@Component
public class RabbitStreamListenerContainerFactory {

    // RabbitMQ Stream 연결과 native Consumer 생성을 담당하는 client Environment
    private final Environment environment;

    // 실패한 Consumer Container의 재시작 작업만 실행하는 전용 scheduler
    private final ScheduledExecutorService restartScheduler;

    // 재시작 반복이 과도하게 발생하지 않도록 설정으로 관리하는 대기 시간
    private final Duration consumerRestartDelay;

    public RabbitStreamListenerContainerFactory(
            final Environment environment,
            final RabbitStreamProperties properties
    ) {
        this.environment = environment;
        this.restartScheduler = Executors.newSingleThreadScheduledExecutor();
        this.consumerRestartDelay = properties.consumerRestartDelay();
    }

    /**
     * 지정한 Stream을 소비할 Container를 생성합니다.
     *
     * @param streamName 구독할 RabbitMQ Stream 이름
     * @param consumerName broker에 저장되는 offset을 식별하는 논리 Consumer 이름
     * @param messageHandler 해당 BC의 도메인 메시지 처리 콜백
     * @return Spring lifecycle에 등록할 Stream Consumer Container
     */
    public StreamListenerContainer create(
            final String streamName,
            final String consumerName,
            final StreamMessageHandler messageHandler
    ) {
        // Spring lifecycle이 관리하고, native RabbitMQ Consumer를 생성·정리하는 Container를 생성
        final StreamListenerContainer container =
                new StreamListenerContainer(environment);

        // 같은 Container가 연속 실패해도 재시작 작업은 한 번만 예약되도록 한다.
        final AtomicBoolean restartScheduled = new AtomicBoolean(false);

        // 이 Container가 구독할 RabbitMQ Stream 이름을 지정
        container.setQueueNames(streamName);

        // Container가 start될 때 생성할 native RabbitMQ Consumer의 설정을 지정
        container.setConsumerCustomizer((id, builder) -> {
            // broker는 이 논리 Consumer 이름별로 마지막 저장 offset을 관리
            builder.name(consumerName);

            // Listener가 context.storeOffset()을 호출할 때만 broker에 offset을 저장
            builder.manualTrackingStrategy();
        });

        // Container에 Listener를 연결
        // 실제 메시지 수신은 Container가 생성한 native Consumer가 수행하고,
        // Listener는 메시지 변환·BC handler 호출·offset 저장을 담당
        container.setupMessageListener(
                new RabbitStreamMessageListener(
                        messageHandler,
                        exception -> scheduleRestart(container, restartScheduled)
                )
        );

        // Spring Container가 시작되기 전에 listener와 consumer 설정을 검증·초기화
        container.afterPropertiesSet();

        return container;
    }

    /**
     * 처리 확정에 실패한 Consumer Container를 설정된 지연 시간 후 재시작
     *
     * <p>Listener가 native Consumer를 이미 닫았으므로, stop으로 기존 Consumer를 정리한 뒤 start로 새 Consumer를 생성합니다.
     * 새 Consumer는 broker에 저장된 마지막 offset부터 읽습니다.</p>
     */
    private void scheduleRestart(
            final StreamListenerContainer container,
            final AtomicBoolean restartScheduled
    ) {
        // 이미 재시작이 예약됐다면 같은 Container에 대한 중복 예약을 막는다.
        if (!restartScheduled.compareAndSet(false, true)) {
            return;
        }

        restartScheduler.schedule(() -> {
            try {
                // Listener가 닫은 native Consumer를 Container에서 정리
                container.stop();
            } finally {
                // 새 Consumer가 기동 직후 다시 실패하면 다음 재시작을 예약할 수 있게 한다.
                restartScheduled.set(false);
            }

            // Container가 새 native Consumer를 생성해, broker에 저장된 offset 다음부터 다시 구독
            container.start();
        }, consumerRestartDelay.toMillis(), TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void close() {
        // 애플리케이션 종료 시 Factory가 생성한 scheduler thread를 함께 정리
        restartScheduler.shutdown();
    }
}
