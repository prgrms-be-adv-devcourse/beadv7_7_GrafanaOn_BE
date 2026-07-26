package shop.dear.commerce.product.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class StoryTest {

    @DisplayName("유효한 값(productImage, content)으로 이야기를 생성할 수 있다.")
    @Test
    void givenValidData_whenCreateStory_thenSuccess() {
        // Given
        final ProductImage productImage = mock(ProductImage.class);
        final String content = "test content";

        // When
        final Story story = Story.create(productImage, content);

        // Then
        assertSoftly(softly -> {
            softly.assertThat(story.getProductImage()).isEqualTo(productImage);
            softly.assertThat(story.getContent()).isEqualTo(content);
        });
    }
}