package shop.dear.commerce.order.purchase.infrastructure.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import shop.dear.commerce.order.purchase.application.port.ProductPort;
import shop.dear.commerce.order.purchase.application.port.dto.ProductInfo;
import shop.dear.commerce.order.purchase.application.port.dto.ProductSaleType;
import shop.dear.commerce.order.purchase.application.port.dto.ProductStatus;
import shop.dear.commerce.order.purchase.infrastructure.client.dto.ProductApiData;
import shop.dear.commerce.order.purchase.infrastructure.client.dto.ProductImageData;
import shop.dear.commerce.order.purchase.infrastructure.client.dto.TradeProductData;
import shop.dear.common.exception.BusinessException;
import shop.dear.common.response.ApiResponse;

import static shop.dear.commerce.order.purchase.domain.exception.PurchaseErrorCode.PRODUCT_LOOKUP_FAILED;
import static shop.dear.commerce.order.purchase.domain.exception.PurchaseErrorCode.PRODUCT_NOT_FOUND;

@Component("purchaseProductHttpClient")
public class ProductHttpClient implements ProductPort {

  private static final String PRODUCT_URI = "/internal/products/{productId}";
  private static final String TRADE_URI = "/internal/products/{productId}/trade";
  private static final String TRADE_CANCEL_URI = "/internal/products/{productId}/trade/cancel";

  private final RestClient restClient;

  public ProductHttpClient(@Qualifier("productRestClient") final RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public ProductInfo getProduct(final Long productId) {
    final ProductApiData data = requestProduct(productId);
    return toProductInfo(data);
  }

  private ProductApiData requestProduct(final Long productId) {
    try {
      final ApiResponse<ProductApiData> response = restClient.get()
              .uri(PRODUCT_URI, productId)
              .retrieve()
              .onStatus(status -> status.value() == 404, (request, responseSpec) -> {
                throw new BusinessException(PRODUCT_NOT_FOUND);
              })
              .onStatus(HttpStatusCode::isError, (request, responseSpec) -> {
                throw new BusinessException(PRODUCT_LOOKUP_FAILED);
              })
              .body(new ParameterizedTypeReference<>() {});

      if (response == null || response.getData() == null) {
        throw new BusinessException(PRODUCT_LOOKUP_FAILED);
      }

      return response.getData();
    } catch (final ResourceAccessException e) {
      throw new BusinessException(PRODUCT_LOOKUP_FAILED, e.getMessage());
    }
  }

  private ProductInfo toProductInfo(final ProductApiData data) {
    return new ProductInfo(
            data.sellerId(),
            data.images().stream()
                    .map(ProductImageData::imageUrl)
                    .toList(),
            data.name(),
            data.brand(),
            data.price(),
            data.modelNumber(),
            data.category(),
            data.releaseDate(),
            ProductSaleType.valueOf(data.saleType()),
            ProductStatus.valueOf(data.status()),
            data.viewCount(),
            data.description(),
            data.insertedAt()
    );
  }

  @Override
  public boolean tradeProduct(final Long productId) {
    final TradeProductData data = requestTradeProduct(productId);
    return data.isChanged();
  }

  private TradeProductData requestTradeProduct(final Long productId) {
    try {
      final ApiResponse<TradeProductData> response = restClient.post()
              .uri(TRADE_URI, productId)
              .retrieve()
              .onStatus(status -> status.value() == 404, (request, responseSpec) -> {
                throw new BusinessException(PRODUCT_NOT_FOUND);
              })
              .onStatus(HttpStatusCode::isError, (request, responseSpec) -> {
                throw new BusinessException(PRODUCT_LOOKUP_FAILED);
              })
              .body(new ParameterizedTypeReference<>() {});

      if (response == null || response.getData() == null) {
        throw new BusinessException(PRODUCT_LOOKUP_FAILED);
      }

      return response.getData();
    } catch (final ResourceAccessException e) {
      throw new BusinessException(PRODUCT_LOOKUP_FAILED, e.getMessage());
    }
  }

  @Override
  public void cancelTradeProduct(final Long productId) {
    try {
      restClient.post()
              .uri(TRADE_CANCEL_URI, productId)
              .retrieve()
              .onStatus(status -> status.value() == 404, (request, responseSpec) -> {
                throw new BusinessException(PRODUCT_NOT_FOUND);
              })
              .onStatus(HttpStatusCode::isError, (request, responseSpec) -> {
                throw new BusinessException(PRODUCT_LOOKUP_FAILED);
              })
              .toBodilessEntity();
    } catch (final ResourceAccessException e) {
      throw new BusinessException(PRODUCT_LOOKUP_FAILED, e.getMessage());
    }
  }
}
