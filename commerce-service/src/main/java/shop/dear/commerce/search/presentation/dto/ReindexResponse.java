package shop.dear.commerce.search.presentation.dto;


import shop.dear.commerce.search.application.port.dto.ReindexResult;

public record ReindexResponse(
        long sourceCount,
        long indexedCount,
        long documentCount,
        // 인덱스 문서 수가 원본 건수와의 차이. 재색인이 덮어쓰기 방식이라 원본에 없는 문서가 남아 생기는 값이다.
        // 다만 두 집합의 크기 차이일 뿐이라 색인 도중 원본이 변경되면 0으로 보일 수도 있다.
        // 정확한 값이 필요하면 ID 비교가 필요하다.
        long excessDocumentCount,
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
