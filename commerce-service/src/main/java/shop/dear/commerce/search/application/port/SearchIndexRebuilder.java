package shop.dear.commerce.search.application.port;

import shop.dear.commerce.search.application.port.dto.ReindexResult;

// 검색 인덱스를 원본 데이터로부터 다시 만든다.
public interface SearchIndexRebuilder {

    ReindexResult rebuild();
}
