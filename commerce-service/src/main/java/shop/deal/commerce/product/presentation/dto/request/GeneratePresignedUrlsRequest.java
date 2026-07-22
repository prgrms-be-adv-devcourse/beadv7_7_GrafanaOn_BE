package shop.deal.commerce.product.presentation.dto.request;

import shop.deal.commerce.product.application.dto.external.GeneratePresignedUrlsCommand;

import java.util.List;

public record GeneratePresignedUrlsRequest(
    List<FileInfo> files
) {
    public record FileInfo(
        int sortOrder,
        String uploadFileType
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
