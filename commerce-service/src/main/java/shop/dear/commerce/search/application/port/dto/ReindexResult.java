package shop.dear.commerce.search.application.port.dto;

/**
 * 정상 처리되었다면 다음 세 값이 모두 일치하여야 합니다.
 *
 * @param sourceCount: search_product 테이블 건수
 * @param indexedCount: 이번 실행에서 색인을 시도한 건수
 * @param documentCount: 색인 후 인덱스에 실제로 존재하는 문서 수
 */
public record ReindexResult(
        long sourceCount,
        long indexedCount,
        long documentCount
) {
}
