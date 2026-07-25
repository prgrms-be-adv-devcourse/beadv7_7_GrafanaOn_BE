package shop.dear.identity.scrap.presentation.dto;

import org.springframework.data.domain.Page;
import shop.dear.identity.scrap.application.dto.ScrapInfo;

import java.util.List;

public record ScrapPageResponse(
    List<ScrapResponse> scrapList,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {
    public static ScrapPageResponse from(final Page<ScrapInfo> page) {
        return new ScrapPageResponse(
            page.getContent()
                .stream()
                .map(ScrapResponse::from)
                .toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext()
        );
    }
}
