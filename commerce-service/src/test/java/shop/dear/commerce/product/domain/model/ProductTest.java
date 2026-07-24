package shop.dear.commerce.product.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.commerce.product.domain.constant.ProductSaleType;
import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static shop.dear.commerce.product.domain.model.Product.PRODUCT_IMAGE_COUNT_LIMIT;

class ProductTest {

    private Product createProduct() {
        return Product.create(
            1L,
            "testName",
            "testBrand",
            "testModelNumber-001",
            ProductCategory.SNEAKERS,
            LocalDate.now(),
            Price.from(BigDecimal.valueOf(120000)),
            ProductSaleType.OFFER,
            "Test Description"
        );
    }

    @DisplayName("상품을 생성한다.")
    @Test
    void givenProductInfo_whenCreate_thenSuccess() {
        //Given
        final Long sellerId = 1L;
        final String name = "testName";
        final String brand = "testBrand";
        final String modelNumber = "testModelNumber-001";
        final ProductCategory category = ProductCategory.SNEAKERS;
        final LocalDate releaseDate = LocalDate.now();
        final Price price = Price.from(BigDecimal.valueOf(120000));
        final ProductSaleType saleType = ProductSaleType.OFFER;
        final String description = "Test Description";

        //When
        final Product product = Product.create(
            sellerId,
            name,
            brand,
            modelNumber,
            category,
            releaseDate,
            price,
            saleType,
            description
        );

        //Then
        assertSoftly(softly -> {
            assertThat(product.getId()).isNull();
            assertThat(product.getSellerId()).isEqualTo(sellerId);
            assertThat(product.getName()).isEqualTo(name);
            assertThat(product.getBrand()).isEqualTo(brand);
            assertThat(product.getModelNumber()).isEqualTo(modelNumber);
            assertThat(product.getCategory()).isEqualTo(category);
            assertThat(product.getReleaseDate()).isEqualTo(releaseDate);
            assertThat(product.getPrice()).isEqualTo(price);
            assertThat(product.getSaleType()).isEqualTo(saleType);
            assertThat(product.getStatus()).isEqualTo(ProductStatus.PREPARING);
            assertThat(product.getViewCount()).isEqualTo(Long.valueOf(0));
            assertThat(product.getDescription()).isEqualTo(description);
        });
    }

    @DisplayName("상품 이름에 null 혹은 공백이 들어오면 예외가 발생한다.")
    @NullAndEmptySource
    @ParameterizedTest
    void givenNameIsNullOrBlank_whenCreate_thenThrowException(final String name) {
        //Given
        final Long sellerId = 1L;
        final String brand = "testBrand";
        final String modelNumber = "testModelNumber-001";
        final ProductCategory category = ProductCategory.SNEAKERS;
        final LocalDate releaseDate = LocalDate.now();
        final Price price = Price.from(BigDecimal.valueOf(120000));
        final ProductSaleType saleType = ProductSaleType.OFFER;
        final String description = "Test Description";

        //Then
        assertThatThrownBy(() -> Product.create(
            sellerId,
            name,
            brand,
            modelNumber,
            category,
            releaseDate,
            price,
            saleType,
            description
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessage("상품 이름으로 null 혹은 공백을 입력할 수 없습니다.");
    }

    @DisplayName("상품 이름이 최대 글자수를 초과하면 예외가 발생한다.")
    @Test
    void givenNameLengthIsExceeded_whenCreate_thenThrowException() {
        //Given
        final Long sellerId = 1L;
        final String name = "a".repeat(Product.MAX_NAME_LENGTH + 1);
        final String brand = "testBrand";
        final String modelNumber = "testModelNumber-001";
        final ProductCategory category = ProductCategory.SNEAKERS;
        final LocalDate releaseDate = LocalDate.now();
        final Price price = Price.from(BigDecimal.valueOf(120000));
        final ProductSaleType saleType = ProductSaleType.OFFER;
        final String description = "Test Description";

        //Then
        assertThatThrownBy(() -> Product.create(
            sellerId,
            name,
            brand,
            modelNumber,
            category,
            releaseDate,
            price,
            saleType,
            description
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessage("상품 이름이 최대 글자수를 초과했습니다.");
    }

    @DisplayName("상품 브랜드에 null 혹은 공백이 들어오면 예외가 발생한다.")
    @NullAndEmptySource
    @ParameterizedTest
    void givenBrandIsNullOrBlank_whenCreate_thenThrowException(final String brand) {
        //Given
        final Long sellerId = 1L;
        final String name = "testName";
        final String modelNumber = "testModelNumber-001";
        final ProductCategory category = ProductCategory.SNEAKERS;
        final LocalDate releaseDate = LocalDate.now();
        final Price price = Price.from(BigDecimal.valueOf(120000));
        final ProductSaleType saleType = ProductSaleType.OFFER;
        final String description = "Test Description";

        //Then
        assertThatThrownBy(() -> Product.create(
            sellerId,
            name,
            brand,
            modelNumber,
            category,
            releaseDate,
            price,
            saleType,
            description
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessage("상품 브랜드로 null 혹은 공백을 입력할 수 없습니다.");
    }

    @DisplayName("상품 브랜드가 최대 글자수를 초과하면 예외가 발생한다.")
    @Test
    void givenBrandLengthIsExceeded_whenCreate_thenThrowException() {
        //Given
        final Long sellerId = 1L;
        final String name = "testName";
        final String brand = "a".repeat(Product.MAX_BRAND_LENGTH + 1);
        final String modelNumber = "testModelNumber-001";
        final ProductCategory category = ProductCategory.SNEAKERS;
        final LocalDate releaseDate = LocalDate.now();
        final Price price = Price.from(BigDecimal.valueOf(120000));
        final ProductSaleType saleType = ProductSaleType.OFFER;
        final String description = "Test Description";

        //Then
        assertThatThrownBy(() -> Product.create(
            sellerId,
            name,
            brand,
            modelNumber,
            category,
            releaseDate,
            price,
            saleType,
            description
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessage("상품 브랜드가 최대 글자수를 초과했습니다.");
    }

    @DisplayName("상품 모델번호에 null 혹은 공백이 들어오면 예외가 발생한다.")
    @NullAndEmptySource
    @ParameterizedTest
    void givenModelNumberIsNullOrBlank_whenCreate_thenThrowException(final String modelNumber) {
        //Given
        final Long sellerId = 1L;
        final String name = "testName";
        final String brand = "testBrand";
        final ProductCategory category = ProductCategory.SNEAKERS;
        final LocalDate releaseDate = LocalDate.now();
        final Price price = Price.from(BigDecimal.valueOf(120000));
        final ProductSaleType saleType = ProductSaleType.OFFER;
        final String description = "Test Description";

        //Then
        assertThatThrownBy(() -> Product.create(
            sellerId,
            name,
            brand,
            modelNumber,
            category,
            releaseDate,
            price,
            saleType,
            description
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessage("상품 모델번호로 null 혹은 공백을 입력할 수 없습니다.");
    }

    @DisplayName("상품 모델번호가 최대 글자수를 초과하면 예외가 발생한다.")
    @Test
    void givenModelNumberLengthIsExceeded_whenCreate_thenThrowException() {
        //Given
        final Long sellerId = 1L;
        final String name = "testName";
        final String brand = "testBrand";
        final String modelNumber = "a".repeat(Product.MAX_MODEL_NUMBER_LENGTH + 1);
        final ProductCategory category = ProductCategory.SNEAKERS;
        final LocalDate releaseDate = LocalDate.now();
        final Price price = Price.from(BigDecimal.valueOf(120000));
        final ProductSaleType saleType = ProductSaleType.OFFER;
        final String description = "Test Description";

        //Then
        assertThatThrownBy(() -> Product.create(
            sellerId,
            name,
            brand,
            modelNumber,
            category,
            releaseDate,
            price,
            saleType,
            description
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessage("상품 모델번호가 최대 글자수를 초과했습니다.");
    }

    @DisplayName("상품 카테고리에 null이 들어오면 예외가 발생한다.")
    @NullSource
    @ParameterizedTest
    void givenCategoryIsNull_whenCreate_thenThrowException(final ProductCategory category) {
        //Given
        final Long sellerId = 1L;
        final String name = "testName";
        final String brand = "testBrand";
        final String modelNumber = "testModelNumber-001";
        final LocalDate releaseDate = LocalDate.now();
        final Price price = Price.from(BigDecimal.valueOf(120000));
        final ProductSaleType saleType = ProductSaleType.OFFER;
        final String description = "Test Description";

        //Then
        assertThatThrownBy(() -> Product.create(
            sellerId,
            name,
            brand,
            modelNumber,
            category,
            releaseDate,
            price,
            saleType,
            description
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessage("상품 카테고리로 null을 입력할 수 없습니다.");
    }

    @DisplayName("상품 발매일에 미래 날짜가 들어오면 예외가 발생한다.")
    @Test
    void givenReleaseDateIsFuture_whenCreate_thenThrowException() {
        //Given
        final Long sellerId = 1L;
        final String name = "testName";
        final String brand = "testBrand";
        final String modelNumber = "testModelNumber-001";
        final ProductCategory category = ProductCategory.SNEAKERS;
        final LocalDate releaseDate = LocalDate.now().plusDays(1);
        final Price price = Price.from(BigDecimal.valueOf(120000));
        final ProductSaleType saleType = ProductSaleType.OFFER;
        final String description = "Test Description";

        //Then
        assertThatThrownBy(() -> Product.create(
            sellerId,
            name,
            brand,
            modelNumber,
            category,
            releaseDate,
            price,
            saleType,
            description
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessage("발매일은 미래 날짜로 설정할 수 없습니다.");
    }

    @DisplayName("상품 판매 유형에 null이 들어오면 예외가 발생한다.")
    @NullSource
    @ParameterizedTest
    void givenSaleTypeIsNull_whenCreate_thenThrowException(final ProductSaleType saleType) {
        //Given
        final Long sellerId = 1L;
        final String name = "testName";
        final String brand = "testBrand";
        final String modelNumber = "testModelNumber-001";
        final ProductCategory category = ProductCategory.SNEAKERS;
        final LocalDate releaseDate = LocalDate.now();
        final Price price = Price.from(BigDecimal.valueOf(120000));
        final String description = "Test Description";

        //Then
        assertThatThrownBy(() -> Product.create(
            sellerId,
            name,
            brand,
            modelNumber,
            category,
            releaseDate,
            price,
            saleType,
            description
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessage("상품 판매방식으로 null을 입력할 수 없습니다.");
    }

    @DisplayName("상품 상세 설명이 최대 글자수를 초과하면 예외가 발생한다.")
    @Test
    void givenDescriptionLengthIsExceeded_whenCreate_thenThrowException() {
        //Given
        final Long sellerId = 1L;
        final String name = "testName";
        final String brand = "testBrand";
        final String modelNumber = "testModelNumber-001";
        final ProductCategory category = ProductCategory.SNEAKERS;
        final LocalDate releaseDate = LocalDate.now();
        final Price price = Price.from(BigDecimal.valueOf(120000));
        final ProductSaleType saleType = ProductSaleType.OFFER;
        final String description = "a".repeat(Product.MAX_DESCRIPTION_LENGTH + 1);

        //Then
        assertThatThrownBy(() -> Product.create(
            sellerId,
            name,
            brand,
            modelNumber,
            category,
            releaseDate,
            price,
            saleType,
            description
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessage("상품 상세설명이 최대 글자수를 초과했습니다.");
    }

    @DisplayName("상품을 수정한다.")
    @Test
    void givenProductInfo_whenUpdate_thenSuccess() {
        //Given
        final Product previousProduct = createProduct();

        //When
        final String name = "realName";
        final String brand = "realBrand";
        final String modelNumber = "realModelNumber-001";
        final ProductCategory category = ProductCategory.BOOTS;
        final LocalDate releaseDate = LocalDate.now();
        final Price price = Price.from(BigDecimal.valueOf(620000));
        final String description = "Real Description";

        final Product product = previousProduct.update(
            name,
            brand,
            modelNumber,
            category,
            releaseDate,
            price,
            description
        );

        //Then
        assertSoftly(softly -> {
            assertThat(product.getName()).isEqualTo(name);
            assertThat(product.getBrand()).isEqualTo(brand);
            assertThat(product.getModelNumber()).isEqualTo(modelNumber);
            assertThat(product.getCategory()).isEqualTo(category);
            assertThat(product.getReleaseDate()).isEqualTo(releaseDate);
            assertThat(product.getPrice()).isEqualTo(price);
            assertThat(product.getDescription()).isEqualTo(description);
        });
    }

    @DisplayName("상품 이미지를 추가한다.")
    @Test
    void givenFileInfo_whenAddImage_thenSuccess() {
        //Given
        final Product product = createProduct();

        //When
        final String url = "https://dear.shop/test.png";
        final int sortOrder = 1;

        product.addImage(url, sortOrder);

        //Then
        final List<ProductImage> images = product.getImages();

        assertThat(images.size()).isEqualTo(1);
        assertThat(images.get(0).getUrl()).isEqualTo(url);
        assertThat(images.get(0).getSortOrder()).isEqualTo(sortOrder);
    }

    @DisplayName("상품 이미지 등록 가능 개수를 초과하면 예외가 발생한다.")
    @Test
    void givenExceededFileCountLimit_whenAddImages_thenThrowException() {
        //Given
        final Product product = createProduct();

        //Then
        assertThatThrownBy(() -> {
            for (int i=0; i<PRODUCT_IMAGE_COUNT_LIMIT + 1; i++) {
                product.addImage("https://dear.shop/test.png", i + 1);
            }
        })
            .isInstanceOf(BusinessException.class)
            .hasMessage("상품 이미지 등록 가능 개수를 초과했습니다.");
    }

    @DisplayName("입력된 값(memberId)이 해당 상품의 판매자가 아니면 예외가 발생한다.")
    @Test
    void givenMemberId_whenValidateOwner_thenSuccess() {
        //Given
        final Long memberId = 2L;
        final Product product = createProduct();  // sellerId = 1L 주입

        //Then
        assertThatThrownBy(() -> product.validateOwner(memberId))
            .isInstanceOf(BusinessException.class)
            .hasMessage("해당 상품의 판매자가 아닙니다.");
    }

    @DisplayName("상품이 수정 가능한 상태인지 여부를 반환한다.")
    @Test
    void whenIsUpdatable_thenReturnResult() {
        //Given
        final Product product1 = createProduct();
        final Product product2 = createProduct();

        //When
        final boolean isUpdatable1 = product1.isUpdatable();  // status = PREPARING

        product2.changeStatusToSoldOut();
        final boolean isUpdatable2 = product2.isUpdatable();  // status = SOLD_OUT

        //Then
        assertThat(isUpdatable1).isEqualTo(true);
        assertThat(isUpdatable2).isEqualTo(false);
    }

    @DisplayName("상품 상태가 sold out으로 변경된다.")
    @Test
    void whenChangeStatusToSoldOut_thenSuccess() {
        //Given
        final Product product = createProduct();

        //When
        product.changeStatusToSoldOut();

        //Then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    @DisplayName("상품 상태가 on sale로 변경된다.")
    @Test
    void whenChangeStatusToOnSale_thenSuccess() {
        //Given
        final Product product = createProduct();

        //When
        product.changeStatusToOnSale();

        //Then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }
}