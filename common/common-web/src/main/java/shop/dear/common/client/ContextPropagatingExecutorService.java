package shop.dear.common.client;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 다른 스레드에서 실행되는 작업에도 요청 컨텍스트를 따라가게 한다.
 *
 * 서킷브레이커는 HTTP 호출을 별도 스레드에서 실행하나, RequestContextHolder와 MDC는 ThreadLocal이다.
 * 1. HTTP 작업을 제출할 때, 요청 스레드의 RequestContextHolder, MDC 맵을 복사해 둔다.
 * 2. 작업 스레드가 작업을 시작할 때, 요청 스레드에서 복사해 뒀던 값들을 넣어준다.
 * 3. 작업이 끝나면, 작업 스레드가 받았던 복사 값들을 비운다.
 */
@RequiredArgsConstructor
public class ContextPropagatingExecutorService implements ExecutorService {

    // 실제로 작업을 수행하는 작업 스레드
    private final ExecutorService delegate;


    private <T> Callable<T> wrap(final Callable<T> task) {
        // 요청 스레드에서 값을 미리 가져온다.
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes(); // 현재 HTTP 요청 정보
        Map<String, String> contextMap = MDC.getCopyOfContextMap(); // traceId를 포함한 전체 MDC 정보 (복사본)

        // 람다 객체는 요청 스레드에서 만들어지고, 아랠 본문은 작업 스레드가 큐에서 꺼낼 때 실행된다.
        return () -> {
            // 다음 두 if 문에서 복사값을 넣는 작업을 실행한다.
            if (attributes != null) {
                RequestContextHolder.setRequestAttributes(attributes);
            }

            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }

            try {
                return task.call();
            } finally {
                //  작업이 끝나면 복사값을 지운다.
                RequestContextHolder.resetRequestAttributes();
                MDC.clear();
            }
        };
    }

    private Runnable wrap(Runnable task) {
        Callable<Void> wrapped = wrap(() -> {
            task.run();
            return null; // Callable은 return 필수. null 처리.
        });

        return () -> {
            try {
                wrapped.call();
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        };
    }

    private <T> Collection<Callable<T>> wrapAll(Collection<? extends Callable<T>> tasks) {
        return tasks.stream()
                .map(task -> wrap((Callable<T>) task))
                .toList();
    }

    // 이제 다음 메서드들은 작업을 인자로 받기 때문에 위에서 만든 wrap 메서드로 인자를 감싸준다.
    @Override
    public @NonNull <T> Future<T> submit(@NonNull Callable<T> task) {
        return delegate.submit(wrap(task));
    }

    @Override
    public @NonNull <T> Future<T> submit(@NonNull Runnable task, T result) {
        return delegate.submit(wrap(task), result);
    }

    @Override
    public @NonNull Future<?> submit(@NonNull Runnable task) {
        return delegate.submit(wrap(task));
    }

    @Override
    public void execute(@NonNull Runnable command) {
        delegate.execute(wrap(command));
    }

    @Override
    public @NonNull <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(wrapAll(tasks));
    }

    @Override
    public @NonNull <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(wrapAll(tasks), timeout, unit);
    }

    @Override
    public @NonNull <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(wrapAll(tasks));
    }

    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(wrapAll(tasks), timeout, unit);
    }

    // 다음 메서드들은 wrap으로 감싸지 않는다. 인자에 작업이 없는 메서드다.
    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public @NonNull List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }
}
