package shop.dear.commerce.cart.infrastructure.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import shop.dear.commerce.cart.application.port.CartProductPort;
import shop.dear.commerce.cart.application.port.dto.CartProductInfo;
import shop.dear.commerce.cart.domain.exception.CartErrorCode;
import shop.dear.commerce.cart.infrastructure.client.dto.CartProductApiData;
import shop.dear.common.exception.BusinessException;
import shop.dear.common.response.ApiResponse;

import java.util.List;

@Component
public class CartProductClient implements CartProductPort {

    private static final String PRODUCT_URI = "/internal/products/{productId}";

    private final RestClient restClient;

    public CartProductClient(@Qualifier("cartRestClient") final RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public CartProductInfo getProduct(final Long productId) {
        final CartProductApiData data = requestProduct(productId);
        return toCartProductInfo(productId, data);
    }

    @Override
    public List<CartProductInfo> getProducts(final List<Long> productIds) {
        return  productIds.stream()
                .map(this::getProduct)
                .toList();
        //TODO: product 벌크 조회API
    }

    private CartProductApiData requestProduct(final Long productId) {
        try {
            final ApiResponse<CartProductApiData> response = restClient.get()
                    .uri(PRODUCT_URI, productId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, responseSpec) -> {
                        throw new BusinessException(CartErrorCode.PRODUCT_NOT_FOUND);
                    })
                    .onStatus(HttpStatusCode::isError, (request, responseSpec) -> {
                        throw new BusinessException(CartErrorCode.PRODUCT_LOOKUP_FAILED);
                    })
                    .body(new ParameterizedTypeReference<ApiResponse<CartProductApiData>>() {
                    });
            if(response == null || response.getData() == null) {
                throw new BusinessException(CartErrorCode.PRODUCT_LOOKUP_FAILED);
            }
            return response.getData();
        } catch(final ResourceAccessException e) {
            throw new BusinessException(CartErrorCode.PRODUCT_LOOKUP_FAILED,e.getMessage());
        }
    }

    private CartProductInfo toCartProductInfo(final Long productId, final CartProductApiData data) {
        return new CartProductInfo(
                productId,
                data.name(),
                data.price(),
                extractThumbnailUrl(data.images()),
                data.status()
        );
    }

    private String extractThumbnailUrl(final List<CartProductApiData.ImageInfo> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.get(0).url();
    }
}