package shop.dear.commerce.product.application.dto.external;

import shop.dear.commerce.product.domain.constant.UploadFileType;

import java.util.List;

public record GeneratePresignedUrlsCommand(
    List<FileInfoCommand> files
) {
    public record FileInfoCommand(
        int sortOrder,
        UploadFileType uploadFileType
    ) {
    }
}
