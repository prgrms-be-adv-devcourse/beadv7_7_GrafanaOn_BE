package shop.dear.commerce.product.application.port;

import shop.dear.commerce.product.domain.constant.UploadFileType;

public interface PresignedUrlGenerator {
    String generate(final int sortOrder, final UploadFileType uploadFileType);
}
