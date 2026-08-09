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
import shop.dear.commerce.order.offer.infrastructure.client.dto.ProductApiData;
import shop.dear.common.exception.BusinessException;
import shop.dear.common.response.ApiResponse;

import java.util.List;

@Component("cartProductClient")
public class CartProductClient implements CartProductPort {

    private static final String PRODUCT_URI = "/internal/products/{productId}";

    private final RestClient restClient;

    public CartProductClient(@Qualifier("cartProductRestClient") final RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public CartProductInfo getProduct(final Long productId) {
        final ProductApiData data = requestProduct(productId);
        return  CartProductInfo.create(data,productId);
    }

    private ProductApiData requestProduct(final Long productId) {
        try {
            final ApiResponse<ProductApiData> response = restClient.get()
                    .uri(PRODUCT_URI, productId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, responseSpec) -> {
                        throw new BusinessException(CartErrorCode.PRODUCT_NOT_FOUND);
                    })
                    .onStatus(HttpStatusCode::isError, (request, responseSpec) -> {
                        throw new BusinessException(CartErrorCode.PRODUCT_LOOKUP_FAILED);
                    })
                    .body(new ParameterizedTypeReference<ApiResponse<ProductApiData>>() {
                    });
            if(response == null || response.getData() == null) {
                throw new BusinessException(CartErrorCode.PRODUCT_LOOKUP_FAILED);
            }
            return response.getData();
        } catch(final ResourceAccessException e) {
            throw new BusinessException(CartErrorCode.PRODUCT_LOOKUP_FAILED,e.getMessage());
        }
    }

    @Override
    public List<CartProductInfo> getProducts(final List<Long> productIds) {
        return  productIds.stream()
                .map(this::getProduct)
                .toList();
        //TODO: product 벌크 조회API
    }

}