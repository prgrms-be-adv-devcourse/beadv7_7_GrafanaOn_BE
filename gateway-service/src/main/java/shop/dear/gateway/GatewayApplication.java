package shop.dear.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// 이게 shop.dear.gateway 하위의 GatewayJwtProperties를 찾아 Bean으로 등록
@ConfigurationPropertiesScan
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
