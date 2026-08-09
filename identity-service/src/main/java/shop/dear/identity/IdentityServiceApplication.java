package shop.dear.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan // 설정 클래스 자동 등록
@SpringBootApplication(scanBasePackages = {
	"shop.dear.common",
	"shop.dear.identity"
})
public class IdentityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentityServiceApplication.class, args);
	}
}
