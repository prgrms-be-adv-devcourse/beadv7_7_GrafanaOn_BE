package shop.dear.commerce.search.application.port.dto;

/**
 * 정상 처리되었다면 sourceCount와 indexedCount가 일치합니다.
 * documentCount는 참고 값입니다.
 *
 * @param sourceCount: search_product 테이블 건수
 * @param indexedCount: 이번 실행에서 색인한 건수. 실패분 포함 X
 * @param documentCount: 색인 후 인덱스에 실제로 존재하는 문서 수
 */
public record ReindexResult(
        long sourceCount,
        long indexedCount,
        long documentCount
) {
}
