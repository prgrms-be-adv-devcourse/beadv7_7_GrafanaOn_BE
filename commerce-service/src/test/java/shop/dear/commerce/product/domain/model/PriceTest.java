package shop.dear.commerce.product.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceTest {

    @DisplayName("0 이상의 유효한 금액으로 Price 객체를 생성할 수 있다.")
    @ParameterizedTest
    @ValueSource(strings = {"0", "1000", "120000.50"})
    void givenValidValue_whenCreatePrice_thenSuccess(final String priceValue) {
        // Given
        final BigDecimal value = new BigDecimal(priceValue);

        // When
        final Price price = Price.from(value);

        // Then
        assertThat(price.getValue()).isEqualTo(value);
    }

    @DisplayName("가격에 null이 들어오면 예외가 발생한다.")
    @Test
    void givenNullValue_whenCreatePrice_thenThrowException() {
        // Given
        final BigDecimal value = null;

        // When & Then
        assertThatThrownBy(() -> Price.from(value))
            .isInstanceOf(BusinessException.class)
            .hasMessage("상품 가격으로 null을 입력할 수 없습니다.");
    }

    @DisplayName("가격이 음수이면 예외가 발생한다.")
    @ParameterizedTest
    @ValueSource(strings = {"-1", "-10000"})
    void givenNegativeValue_whenCreatePrice_thenThrowException(final String negativeValue) {
        // Given
        final BigDecimal value = new BigDecimal(negativeValue);

        // When & Then
        assertThatThrownBy(() -> Price.from(value))
            .isInstanceOf(BusinessException.class)
            .hasMessage("상품 가격은 음수일 수 없습니다.");
    }

    @DisplayName("동일한 금액을 가진 Price 객체는 동등하다 (EqualsAndHashCode).")
    @Test
    void givenSameValue_whenEquals_thenTrue() {
        // Given
        final Price price1 = Price.from(BigDecimal.valueOf(10000));
        final Price price2 = Price.from(BigDecimal.valueOf(10000));

        // When & Then
        assertThat(price1).isEqualTo(price2);
        assertThat(price1.hashCode()).isEqualTo(price2.hashCode());
    }
}