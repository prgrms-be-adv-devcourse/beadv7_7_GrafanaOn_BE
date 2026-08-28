package shop.dear.commerce.financial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = {
	"shop.dear.common",
	"shop.dear.commerce.financial"
})
public class FinancialServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancialServiceApplication.class, args);
	}
}
