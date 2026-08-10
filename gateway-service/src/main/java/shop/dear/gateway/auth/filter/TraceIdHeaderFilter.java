package shop.dear.gateway.auth.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@Component
public class TraceIdHeaderFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
        final String traceId = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER))
            .orElseGet(() -> UUID.randomUUID().toString().substring(0, 8));

        final ServerHttpRequest request = exchange.getRequest().mutate()
            .header(TRACE_ID_HEADER, traceId)
            .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}