package shop.dear.commerce.product.application.fake;

import shop.dear.commerce.product.application.port.PresignedUrlGenerator;
import shop.dear.commerce.product.domain.constant.UploadFileType;

public class FakePresignedUrlGenerator implements PresignedUrlGenerator {
    @Override
    public String generate(final int sortOrder, final UploadFileType uploadFileType) {
        return "https://fake-s3-bucket.com/test/" + sortOrder + "." + uploadFileType.getExtension();
    }
}
