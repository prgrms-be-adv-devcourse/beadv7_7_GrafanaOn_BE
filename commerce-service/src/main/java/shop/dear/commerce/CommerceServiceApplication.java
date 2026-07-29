package shop.dear.commerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
	"shop.dear.common",
	"shop.dear.commerce"
})
public class CommerceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommerceServiceApplication.class, args);
	}
}
