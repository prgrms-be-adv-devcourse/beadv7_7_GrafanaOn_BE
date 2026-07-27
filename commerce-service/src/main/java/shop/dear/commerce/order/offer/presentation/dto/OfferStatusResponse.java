package shop.dear.commerce.order.offer.presentation.dto;

public record OfferStatusResponse(boolean exists) {

  public static OfferStatusResponse from(final boolean exists) {
    return new OfferStatusResponse(exists);
  }
}
