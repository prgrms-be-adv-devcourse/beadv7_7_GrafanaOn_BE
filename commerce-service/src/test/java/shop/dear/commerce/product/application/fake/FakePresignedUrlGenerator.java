package shop.dear.commerce.product.application.fake;

import shop.dear.commerce.product.application.port.PresignedUrlGenerator;

public class FakePresignedUrlGenerator implements PresignedUrlGenerator {
    @Override
    public String generate(final int sortOrder, final String uploadFileType) {
        return "https://fake-s3-bucket.com/test/" + sortOrder + "." + uploadFileType.toLowerCase();
    }
}
