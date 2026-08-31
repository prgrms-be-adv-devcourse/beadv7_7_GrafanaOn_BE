package shop.dear.commerce.product.infrastructure.messaging;

final class FinishedOrderStream {

    static final String NAME = "order.finished";

    static final String EVENT_TYPE = "FinishedOrderEvent";

    static final String CONSUMER_NAME = "product-service-finished-order-sold-out-group";

    private FinishedOrderStream() {
    }
}
