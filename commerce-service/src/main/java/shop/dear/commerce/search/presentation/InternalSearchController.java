package shop.dear.commerce.search.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.commerce.search.application.port.SearchIndexRebuilder;
import shop.dear.commerce.search.presentation.dto.ReindexResponse;
import shop.dear.common.response.ApiResponse;

import static shop.dear.common.response.ApiResponse.successWithData;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/search")
public class InternalSearchController {

    private final SearchIndexRebuilder searchIndexRebuilder;

    @PostMapping("/reindex")
    public ResponseEntity<ApiResponse<ReindexResponse>> reindex() {
        return ResponseEntity.ok(successWithData(ReindexResponse.from(searchIndexRebuilder.rebuild())));
    }
}
