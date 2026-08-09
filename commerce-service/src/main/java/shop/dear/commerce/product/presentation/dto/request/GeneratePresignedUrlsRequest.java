package shop.dear.commerce.product.presentation.dto.request;

import shop.dear.commerce.product.application.dto.command.GeneratePresignedUrlsCommand;
import shop.dear.commerce.product.domain.constant.UploadFileType;

import java.util.List;

public record GeneratePresignedUrlsRequest(
    List<FileInfo> files
) {
    public record FileInfo(
        int sortOrder,
        UploadFileType uploadFileType
    ) {
    }

    public GeneratePresignedUrlsCommand toCommand() {
        final List<GeneratePresignedUrlsCommand.FileInfoCommand> files = this.files.stream()
            .map(file -> new GeneratePresignedUrlsCommand.FileInfoCommand(
                file.sortOrder,
                file.uploadFileType
            ))
            .toList();

        return new GeneratePresignedUrlsCommand(files);
    }
}
