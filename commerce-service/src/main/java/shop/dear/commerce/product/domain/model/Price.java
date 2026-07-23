package shop.dear.commerce.product.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.commerce.product.domain.exception.ProductErrorCode;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Price {

    @Column(name = "price", precision = 15, scale = 2, nullable = false)
    private BigDecimal value;

    private Price(final BigDecimal value) {
        validate(value);

        this.value = value;
    }

    public static Price from(final BigDecimal value) {
        return new Price(value);
    }

    private void validate(final BigDecimal value) {
        if (value == null) {
            throw new BusinessException(ProductErrorCode.INVALID_PRODUCT_PRICE);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ProductErrorCode.PRODUCT_PRICE_CANNOT_BE_NEGATIVE);
        }
    }
}
