package shop.dear.commerce.search.presentation.dto;


import shop.dear.commerce.search.application.port.dto.ReindexResult;

public record ReindexResponse(
        long sourceCount,
        long indexedCount,
        long documentCount,
        boolean matched
) {
    public static ReindexResponse from(final ReindexResult result) {
        return new ReindexResponse(
                result.sourceCount(),
                result.indexedCount(),
                result.documentCount(),
                result.sourceCount() == result.documentCount()
        );
    }
}
