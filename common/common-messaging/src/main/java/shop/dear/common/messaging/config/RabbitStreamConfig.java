package shop.dear.common.messaging.config;

import com.rabbitmq.stream.Environment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RabbitStreamProperties.class)
@ConditionalOnProperty(prefix = "messaging.rabbitmq.stream", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitStreamConfig {

    @Bean(destroyMethod = "close")
    public Environment rabbitStreamEnvironment(
            RabbitStreamProperties properties
    ) {
        return Environment.builder()
                .host(properties.host())
                .port(properties.port())
                .username(properties.username())
                .password(properties.password())
                .virtualHost(properties.virtualHost())
                .build();
    }
}
