package shop.dear.commerce.product.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.dear.commerce.product.domain.exception.ProductImageErrorCode;
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

    @JsonCreator
    public static UploadFileType from(final String value) {
        return Arrays.stream(UploadFileType.values())
            .filter(type -> type.name().equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new BusinessException(ProductImageErrorCode.INVALID_UPLOAD_FILE_TYPE));
    }
}
