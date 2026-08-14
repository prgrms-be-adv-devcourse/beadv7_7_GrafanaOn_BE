package shop.dear.commerce.order.purchase.application.port.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Purchase 도메인의 Product enum 동기화 검증")
class ProductEnumContractTest {

    @DisplayName("Purchase의 ProductSaleType은 Product 도메인의 ProductSaleType과 동일한 값을 가져야 한다")
    @Test
    void productSaleType_shouldBeConsistentWithProductDomain() {
        Set<String> productDomainValues = Arrays.stream(
                        shop.dear.commerce.product.domain.constant.ProductSaleType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        Set<String> purchaseValues = Arrays.stream(ProductSaleType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(purchaseValues)
                .as("Purchase의 ProductSaleType이 Product 도메인의 ProductSaleType과 일치해야 합니다. "
                        + "Product 도메인에 새로운 SaleType이 추가되었다면 Purchase 쪽에도 반영해주세요.")
                .isEqualTo(productDomainValues);
    }

    @DisplayName("Purchase의 ProductStatus는 Product 도메인의 ProductStatus와 동일한 값을 가져야 한다")
    @Test
    void productStatus_shouldBeConsistentWithProductDomain() {
        Set<String> productDomainValues = Arrays.stream(
                        shop.dear.commerce.product.domain.constant.ProductStatus.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        Set<String> purchaseValues = Arrays.stream(ProductStatus.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(purchaseValues)
                .as("Purchase의 ProductStatus가 Product 도메인의 ProductStatus와 일치해야 합니다. "
                        + "Product 도메인에 새로운 Status가 추가되었다면 Purchase 쪽에도 반영해주세요.")
                .isEqualTo(productDomainValues);
    }
}
