package shop.dear.common.messaging.consumer;

import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.OffsetSpecification;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;
import org.springframework.stereotype.Component;
import shop.dear.common.messaging.config.RabbitStreamProperties;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RabbitMQ Stream Consumer Container를 생성하고, 처리 실패 후 재시작을 관리하는 Factory
 *
 * <p>각 Consumer는 수동 offset 추적을 사용합니다.
 * 처리 실패 시 Listener가 native Consumer를 닫고,
 * Factory는 설정된 시간 뒤 Container를 재시작합니다.</p>
 */
@Component
@ConditionalOnProperty(prefix = "messaging.rabbitmq.stream", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitStreamListenerContainerFactory {

    private static final Logger log =
            LoggerFactory.getLogger(RabbitStreamListenerContainerFactory.class);

    // RabbitMQ Stream 연결과 native Consumer 생성을 담당하는 client Environment
    private final Environment environment;

    // Container 재시작 연속 실패 시 허용하는 최대 시도 횟수
    private final int consumerRestartMaxAttempts;

    // 실패한 Consumer Container의 재시작 작업만 실행하는 전용 scheduler
    private final ScheduledExecutorService restartScheduler;

    // 재시작 반복이 과도하게 발생하지 않도록 설정으로 관리하는 대기 시간
    private final Duration consumerRestartDelay;

    public RabbitStreamListenerContainerFactory(
            final Environment environment,
            final RabbitStreamProperties properties
    ) {
        this.environment = environment;
        this.consumerRestartMaxAttempts = properties.consumerRestartMaxAttempts();
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

        // 이 Container의 stop/start가 연속으로 실패한 횟수
        // 재시작에 성공하면 0으로 초기화
        final AtomicInteger consecutiveRestartFailures = new AtomicInteger(0);

        // 이 Container가 구독할 RabbitMQ Stream 이름을 지정
        container.setQueueNames(streamName);

        // Container가 start될 때 생성할 native RabbitMQ Consumer의 설정을 지정
        container.setConsumerCustomizer((id, builder) -> {
            // broker는 이 논리 Consumer 이름별로 마지막 저장 offset을 관리
            builder.name(consumerName);

            // 저장된 offset이 없는 최초 Consumer는 Stream의 첫 메시지부터 소비
            // 이미 저장된 offset이 있으면 broker가 해당 위치를 기준으로 구독 재개
            builder.offset(OffsetSpecification.first());

            // Listener가 context.storeOffset()을 호출할 때만 broker에 offset을 저장
            builder.manualTrackingStrategy();
        });

        // Container에 Listener를 연결
        // 실제 메시지 수신은 Container가 생성한 native Consumer가 수행하고,
        // Listener는 메시지 변환·BC handler 호출·offset 저장을 담당
        container.setupMessageListener(
                new RabbitStreamMessageListener(
                        messageHandler,
                        exception -> scheduleRestart(
                                container,
                                consumerName,
                                restartScheduled,
                                consecutiveRestartFailures
                        )
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
            final String consumerName,
            final AtomicBoolean restartScheduled,
            final AtomicInteger consecutiveRestartFailures
    ) {
        // 최대 재시작 실패 횟수에 도달한 Consumer는 자동 재시작하지 않는다.
        if (consecutiveRestartFailures.get() >= consumerRestartMaxAttempts) {
            return;
        }

        // 이미 재시작이 예약됐다면 같은 Container에 대한 중복 예약을 막는다.
        if (!restartScheduled.compareAndSet(false, true)) {
            return;
        }

        restartScheduler.schedule(() -> {
            try {
                // Listener가 닫은 native Consumer를 Container에서 정리
                container.stop();

                // Container가 새 native Consumer를 생성해, broker에 저장된 offset 다음부터 다시 구독
                container.start();

                // 재시작에 성공하면 연속 실패 횟수를 초기화한다.
                consecutiveRestartFailures.set(0);
            } catch (RuntimeException exception) {
                final int failedAttempts = consecutiveRestartFailures.incrementAndGet();

                if (failedAttempts >= consumerRestartMaxAttempts) {
                    log.error(
                            "RabbitMQ Stream Consumer 재시작이 최대 횟수만큼 실패했습니다. "
                                    + "consumerName={}, maxAttempts={}",
                            consumerName,
                            consumerRestartMaxAttempts,
                            exception
                    );
                } else {
                    log.warn(
                            "RabbitMQ Stream Consumer 재시작에 실패했습니다. "
                                    + "다음 재시작을 예약합니다. consumerName={}, attempt={}/{}",
                            consumerName,
                            failedAttempts,
                            consumerRestartMaxAttempts,
                            exception
                    );

                    // Container 중지 또는 시작에 실패하면 다음 재시작을 다시 예약
                    restartScheduled.set(false);
                    scheduleRestart(
                            container,
                            consumerName,
                            restartScheduled,
                            consecutiveRestartFailures
                    );
                    return;
                }
            }

            // 현재 재시작 작업을 종료한다.
            // 최대 실패 도달 여부는 consecutiveRestartFailures로 판단한다.
            restartScheduled.set(false);
        }, consumerRestartDelay.toMillis(), TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void close() {
        // 애플리케이션 종료 시 Factory가 생성한 scheduler thread를 함께 정리
        restartScheduler.shutdown();
    }
}
