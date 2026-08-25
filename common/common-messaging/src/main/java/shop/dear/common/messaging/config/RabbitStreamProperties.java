package shop.dear.common.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "messaging.rabbitmq.stream")
public record RabbitStreamProperties(
        String host,

        @DefaultValue("5552")
        int port,

        String username,
        String password,

        @DefaultValue("/")
        String virtualHost
) {

}
