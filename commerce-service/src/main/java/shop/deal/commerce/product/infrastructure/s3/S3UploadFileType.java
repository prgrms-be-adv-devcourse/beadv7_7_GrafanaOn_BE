package shop.deal.commerce.product.infrastructure.s3;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.deal.commerce.product.domain.exception.ProductErrorCode;
import shop.deal.common.exception.BusinessException;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum S3UploadFileType {

    JPG("image/jpeg", "jpg"),
    JPEG("image/jpeg", "jpeg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    ;

    private final String contentType;
    private final String extension;

    public static S3UploadFileType from(final String extension) {
        return Arrays.stream(values())
            .filter(type -> type.getExtension().equalsIgnoreCase(extension))
            .findFirst()
            .orElseThrow(() -> new BusinessException(ProductErrorCode.INVALID_PRODUCT_IMAGE_TYPE));
    }
}
