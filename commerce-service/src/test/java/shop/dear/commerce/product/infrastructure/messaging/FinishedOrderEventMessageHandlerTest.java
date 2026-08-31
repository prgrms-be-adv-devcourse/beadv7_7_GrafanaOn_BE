package shop.dear.commerce.product.infrastructure.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.product.application.ProductService;
import shop.dear.commerce.product.infrastructure.inbox.ProductInboxRepository;
import shop.dear.common.messaging.consumer.StreamMessage;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FinishedOrderEventMessageHandlerTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Mock
    private ProductInboxRepository inboxRepository;

    @Mock
    private ProductService productService;

    private FinishedOrderEventMessageHandler handler;

    private static final String VALID_PAYLOAD =
        """
        {"orderId":1,"buyerId":2,"sellerId":3,"productId":42,"amount":10000,"orderType":"PURCHASE"}
        """;

    @DisplayName("신규 FinishedOrderEvent를 받으면 Inbox에 적재하고 상품 판매완료 처리를 호출한다.")
    @Test
    void givenNewFinishedOrderEvent_whenHandle_thenCompletesProductSale() {
        //Given
        handler = new FinishedOrderEventMessageHandler(inboxRepository, objectMapper, productService);
        given(inboxRepository.insertIfAbsent(
            FinishedOrderStream.CONSUMER_NAME, "event-1", FinishedOrderStream.EVENT_TYPE, VALID_PAYLOAD
        )).willReturn(1);
        final StreamMessage message = new StreamMessage("event-1", FinishedOrderStream.EVENT_TYPE, VALID_PAYLOAD, 0L);

        //When
        handler.handle(message);

        //Then
        then(productService).should().completeProductSale(42L);
    }

    @DisplayName("이미 Inbox에 있는 중복 이벤트는 도메인 로직을 실행하지 않고 정상 반환한다.")
    @Test
    void givenDuplicateEvent_whenHandle_thenSkipsDomainLogic() {
        //Given
        handler = new FinishedOrderEventMessageHandler(inboxRepository, objectMapper, productService);
        given(inboxRepository.insertIfAbsent(
            FinishedOrderStream.CONSUMER_NAME, "event-1", FinishedOrderStream.EVENT_TYPE, VALID_PAYLOAD
        )).willReturn(0);
        final StreamMessage message = new StreamMessage("event-1", FinishedOrderStream.EVENT_TYPE, VALID_PAYLOAD, 0L);

        //When
        handler.handle(message);

        //Then
        then(productService).should(never()).completeProductSale(anyLong());
    }

    @DisplayName("알 수 없는 eventType은 Inbox 조회 없이 무시하고 정상 반환한다.")
    @Test
    void givenUnknownEventType_whenHandle_thenIgnoresMessage() {
        //Given
        handler = new FinishedOrderEventMessageHandler(inboxRepository, objectMapper, productService);
        final StreamMessage message = new StreamMessage("event-1", "SomeOtherEvent", VALID_PAYLOAD, 0L);

        //When
        handler.handle(message);

        //Then
        then(inboxRepository).should(never()).insertIfAbsent(anyString(), anyString(), anyString(), anyString());
        then(productService).should(never()).completeProductSale(anyLong());
    }

    @DisplayName("신규 이벤트인데 페이로드 역직렬화에 실패하면 예외를 던져 offset이 저장되지 않게 한다.")
    @Test
    void givenMalformedPayload_whenHandle_thenThrows() {
        //Given
        handler = new FinishedOrderEventMessageHandler(inboxRepository, objectMapper, productService);
        given(inboxRepository.insertIfAbsent(
            FinishedOrderStream.CONSUMER_NAME, "event-1", FinishedOrderStream.EVENT_TYPE, "not-json"
        )).willReturn(1);
        final StreamMessage message = new StreamMessage("event-1", FinishedOrderStream.EVENT_TYPE, "not-json", 0L);

        //When & Then
        assertThatThrownBy(() -> handler.handle(message))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("판매완료 처리가 실패하면 예외를 그대로(래핑 없이) 전파한다.")
    @Test
    void givenCompleteProductSaleFails_whenHandle_thenPropagatesOriginalException() {
        //Given
        handler = new FinishedOrderEventMessageHandler(inboxRepository, objectMapper, productService);
        given(inboxRepository.insertIfAbsent(
            FinishedOrderStream.CONSUMER_NAME, "event-1", FinishedOrderStream.EVENT_TYPE, VALID_PAYLOAD
        )).willReturn(1);
        willThrow(new IllegalStateException("DB 오류"))
            .given(productService).completeProductSale(42L);
        final StreamMessage message = new StreamMessage("event-1", FinishedOrderStream.EVENT_TYPE, VALID_PAYLOAD, 0L);

        //When & Then
        assertThatThrownBy(() -> handler.handle(message))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("DB 오류");
    }
}
