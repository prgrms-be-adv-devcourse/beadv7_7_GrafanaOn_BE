package shop.deal.commerce.product.infrastructure.s3.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@Configuration
public class S3Config {

    @Value("${secret.aws.region}")
    private String region;

    @Value("${secret.aws.credentials.access-key}")
    private String accessKey;

    @Value("${secret.aws.credentials.secret-key}")
    private String secretKey;

    @Bean
    public S3Presigner s3Presigner() {
        final S3Presigner s3Presigner = S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            .build();

        log.info("s3 presigner 초기화 성공 : {}", s3Presigner);

        return s3Presigner;
    }
}
