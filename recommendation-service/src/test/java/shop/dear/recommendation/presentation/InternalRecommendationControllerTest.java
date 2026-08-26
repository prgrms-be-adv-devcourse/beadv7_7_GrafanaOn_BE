package shop.dear.recommendation.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.dear.recommendation.infrastructure.inbox.InboxService;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalRecommendationController.class)
class InternalRecommendationControllerTest {

    private static final String URI = "/internal/recommendation/product-events";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InboxService inboxService;

    @Test
    @DisplayName("정상 요청이면 200을 반환하고 inbox에 적재를 위임한다")
    void acceptsValidRequest() throws Exception {
        final String body = """
            [
              {
                "id": 1,
                "aggregateType": "Product",
                "aggregateId": "100",
                "eventType": "PRODUCT_UPDATED",
                "payload": { "productId": 100, "story": "이야기" },
                "occurredAt": "2026-08-25T12:00:00"
              }
            ]
            """;

        mockMvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());

        then(inboxService).should().saveProductEvents(anyList());
    }

    @Test
    @DisplayName("payload가 없으면 400을 반환하고 적재를 시도하지 않는다")
    void rejectsMissingPayload() throws Exception {
        final String body = """
            [
              {
                "id": 1,
                "aggregateType": "Product",
                "aggregateId": "100",
                "eventType": "PRODUCT_UPDATED",
                "occurredAt": "2026-08-25T12:00:00"
              }
            ]
            """;

        mockMvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());

        then(inboxService).should(never()).saveProductEvents(anyList());
    }

    @Test
    @DisplayName("멱등키인 id가 없으면 400을 반환한다")
    void rejectsMissingId() throws Exception {
        final String body = """
            [
              {
                "aggregateType": "Product",
                "aggregateId": "100",
                "eventType": "PRODUCT_UPDATED",
                "payload": { "productId": 100 },
                "occurredAt": "2026-08-25T12:00:00"
              }
            ]
            """;

        mockMvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());

        then(inboxService).should(never()).saveProductEvents(anyList());
    }

    @Test
    @DisplayName("빈 배열을 받아도 200을 반환한다")
    void acceptsEmptyArray() throws Exception {
        mockMvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON).content("[]"))
            .andExpect(status().isOk());
    }
}
