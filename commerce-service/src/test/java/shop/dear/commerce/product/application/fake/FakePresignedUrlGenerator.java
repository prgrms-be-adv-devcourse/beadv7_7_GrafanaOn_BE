package shop.dear.commerce.product.application.fake;

import lombok.extern.slf4j.Slf4j;
import shop.dear.commerce.product.application.port.PresignedUrlGenerator;
import shop.dear.commerce.product.domain.constant.UploadFileType;

@Slf4j
public class FakePresignedUrlGenerator implements PresignedUrlGenerator {
    @Override
    public String generate(final int sortOrder, final UploadFileType uploadFileType) {
        final String url = "https://fake-s3-bucket.com/test/" + sortOrder + "." + uploadFileType.getExtension();

        log.info("[FakePresignedUrlGenerator] generate presigned url: {}", url);
        return url;
    }
}
