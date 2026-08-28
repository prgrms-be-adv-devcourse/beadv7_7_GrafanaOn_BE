package shop.dear.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "shop.dear.recommendation",
        "shop.dear.common"
})
public class RecommendationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendationServiceApplication.class, args);
    }
}
