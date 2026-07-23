package shop.dear.commerce.product.domain.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.commerce.product.domain.exception.ProductErrorCode;
import shop.dear.common.exception.BusinessException;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum UploadFileType {

    JPG("image/jpeg", "jpg"),
    JPEG("image/jpeg", "jpeg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    ;

    private final String contentType;
    private final String extension;

    public static UploadFileType from(final String extension) {
        return Arrays.stream(values())
            .filter(type -> type.getExtension().equalsIgnoreCase(extension))
            .findFirst()
            .orElseThrow(() -> new BusinessException(ProductErrorCode.INVALID_PRODUCT_IMAGE_TYPE));
    }
}
