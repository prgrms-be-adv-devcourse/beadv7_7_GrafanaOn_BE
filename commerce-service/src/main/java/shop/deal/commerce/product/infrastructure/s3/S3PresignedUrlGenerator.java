package shop.deal.commerce.product.infrastructure.s3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import shop.deal.commerce.product.application.port.PresignedUrlGenerator;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class S3PresignedUrlGenerator implements PresignedUrlGenerator {

    private static final String PRODUCT_IMAGE_ROOT_DIR_NAME = "product_image";
    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 5;

    @Value("${secret.aws.s3.bucket-name}")
    private String bucketName;

    private final S3Presigner s3Presigner;

    @Override
    public String generate(final int sortOrder, final String uploadFileType) {
        return s3Presigner.presignPutObject(buildPutObjectPresignRequest(sortOrder, uploadFileType))
            .url()
            .toExternalForm();
    }

    private PutObjectPresignRequest buildPutObjectPresignRequest(final int sortOrder, final String uploadFileType) {
        final S3UploadFileType s3UploadFileType = S3UploadFileType.from(uploadFileType);

        final String s3ObjectKey = String.format("%s/%s/%d.%s",
            PRODUCT_IMAGE_ROOT_DIR_NAME,
            UUID.randomUUID(),
            sortOrder,
            s3UploadFileType.getExtension()
        );

        log.info("generate s3 put object key: {}", s3ObjectKey);

        final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(s3ObjectKey)
            .contentType(s3UploadFileType.getContentType())
            .build();

        return PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(PRESIGNED_URL_EXPIRATION_MINUTES))
            .putObjectRequest(putObjectRequest)
            .build();
    }
}
