package shop.deal.commerce.product.application.dto.external;

import java.util.List;

public record GeneratePresignedUrlsCommand(
    List<FileInfoCommand> files
) {
    public record FileInfoCommand(
        int sortOrder,
        String uploadFileType
    ) {
    }
}
