package shop.dear.commerce.search.presentation.dto;


import shop.dear.commerce.search.application.port.dto.ReindexResult;

public record ReindexResponse(
        long sourceCount,
        long indexedCount,
        long documentCount,
        long orphanCount,
        boolean fullyIndexed
) {
    public static ReindexResponse from(final ReindexResult result) {
        return new ReindexResponse(
                result.sourceCount(),
                result.indexedCount(),
                result.documentCount(),
                Math.max(0, result.documentCount() - result.sourceCount()),
                result.sourceCount() == result.indexedCount()
        );
    }
}
