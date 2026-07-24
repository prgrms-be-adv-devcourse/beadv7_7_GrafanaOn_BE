package shop.dear.commerce.product.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import shop.dear.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

class ProductImageTest {

    @DisplayName("유효한 값(product, url, sortOrder)으로 상품 이미지를 생성할 수 있다.")
    @Test
    void givenValidData_whenCreateProductImage_thenSuccess() {
        // Given
        final Product product = mock(Product.class);
        final String url = "https://dear.shop/test.png";
        final int sortOrder = 1;

        // When
        final ProductImage productImage = ProductImage.create(product, url, sortOrder);

        // Then
        assertSoftly(softly -> {
            softly.assertThat(productImage.getProduct()).isEqualTo(product);
            softly.assertThat(productImage.getUrl()).isEqualTo(url);
            softly.assertThat(productImage.getSortOrder()).isEqualTo(sortOrder);
            softly.assertThat(productImage.getStory()).isNull();
        });
    }

    @DisplayName("상품 이미지 URL에 null 혹은 공백이 들어오면 예외가 발생한다.")
    @NullAndEmptySource
    @ParameterizedTest
    void givenUrlIsNullOrBlank_whenCreateProductImage_thenThrowException(final String invalidUrl) {
        // Given
        final Product product = mock(Product.class);
        final int sortOrder = 1;

        // When & Then
        assertThatThrownBy(() -> ProductImage.create(product, invalidUrl, sortOrder))
            .isInstanceOf(BusinessException.class)
            .hasMessage("URL로 null 혹은 공백을 입력할 수 없습니다.");
    }

    @DisplayName("이미지 URL 길이가 최대 글자수를 초과하면 예외가 발생한다.")
    @Test
    void givenUrlLengthIsExceeded_whenCreateProductImage_thenThrowException() {
        // Given
        final Product product = mock(Product.class);
        final String exceededUrl = "https://dear.shop/" + "a".repeat(501);
        final int sortOrder = 1;

        // Then
        assertThatThrownBy(() -> ProductImage.create(product, exceededUrl, sortOrder))
            .isInstanceOf(BusinessException.class)
            .hasMessage("URL이 최대 글자수를 초과했습니다.");
    }

    @DisplayName("이미지 정렬 순서가 최소 이미지 등록 가능 개수 미만이면 예외가 발생한다.")
    @Test
    void givenMinimumSortOrderLimit_whenCreateProductImage_thenThrowException() {
        // Given
        final Product product = mock(Product.class);
        final String url = "https://dear.shop/test.png";
        final int exceededSortOrder = Product.PRODUCT_IMAGE_COUNT_LIMIT + 1;

        // When & Then
        assertThatThrownBy(() -> ProductImage.create(product, url, exceededSortOrder))
            .isInstanceOf(BusinessException.class)
            .hasMessage("이미지 정렬 순서가 최대 이미지 개수를 초과했습니다.");
    }

    @DisplayName("이미지 정렬 순서가 최대 이미지 등록 가능 개수를 초과하면 예외가 발생한다.")
    @Test
    void givenSortOrderExceedsLimit_whenCreateProductImage_thenThrowException() {
        // Given
        final Product product = mock(Product.class);
        final String url = "https://dear.shop/test.png";
        final int exceededSortOrder = Product.PRODUCT_IMAGE_COUNT_LIMIT + 1;

        // When & Then
        assertThatThrownBy(() -> ProductImage.create(product, url, exceededSortOrder))
            .isInstanceOf(BusinessException.class)
            .hasMessage("이미지 정렬 순서가 최대 이미지 개수를 초과했습니다.");
    }

    @DisplayName("상품 이미지에 이야기를 추가한다.")
    @Test
    void givenContent_whenAddStory_thenStoryIsCreated() {
        // Given
        final Product product = mock(Product.class);
        final ProductImage productImage = ProductImage.create(product, "https://dear.shop/test.png", 1);
        final String storyContent = "스토리 상세 내용";

        // When
        productImage.addStory(storyContent);

        // Then
        assertThat(productImage.getStory()).isNotNull();
        assertThat(productImage.getStory().getContent()).isEqualTo(storyContent);
    }
}