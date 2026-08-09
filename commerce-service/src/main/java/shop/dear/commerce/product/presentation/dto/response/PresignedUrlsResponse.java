package shop.dear.commerce.product.presentation.dto.response;

import shop.dear.commerce.product.application.dto.PresignedUrlInfoDto;

import java.util.List;

public record PresignedUrlsResponse(
    List<PresignedUrl> presignedUrls
) {
    public record PresignedUrl(
        int sortOrder,
        String presignedUrl
    ) {
    }

    public static PresignedUrlsResponse of(final List<PresignedUrlInfoDto> presignedUrls) {
        final List<PresignedUrl> urls = presignedUrls.stream()
            .map(url -> new PresignedUrlsResponse.PresignedUrl(
                url.sortOrder(),
                url.presignedUrl()
            ))
            .toList();

        return new PresignedUrlsResponse(urls);
    }
}
