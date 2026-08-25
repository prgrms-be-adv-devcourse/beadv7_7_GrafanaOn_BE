package shop.dear.commerce.product.outbox;

public record ProductOutboxPayload(
    Long productId,
    String story
) {

    public static ProductOutboxPayload of(final Long productId, final String story) {
        return new ProductOutboxPayload(productId, story);
    }
}
