package shop.dear.commerce.product.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.product.application.dto.GetProductDetailDto;
import shop.dear.commerce.product.application.dto.GetSellerProductDto;
import shop.dear.commerce.product.application.dto.MemberProductExistsDto;
import shop.dear.commerce.product.application.dto.PresignedUrlInfoDto;
import shop.dear.commerce.product.application.dto.ScrapProductInfoDto;
import shop.dear.commerce.product.application.dto.command.CreateProductCommand;
import shop.dear.commerce.product.application.dto.command.GetScrapProductCommand;
import shop.dear.commerce.product.application.dto.command.UpdateProductCommand;
import shop.dear.commerce.product.application.dto.command.GeneratePresignedUrlsCommand;
import shop.dear.commerce.product.application.fake.FakeMemberPort;
import shop.dear.commerce.product.application.fake.FakeOfferPort;
import shop.dear.commerce.product.application.fake.FakePresignedUrlGenerator;
import shop.dear.commerce.product.application.fake.FakeProductEventPublisher;
import shop.dear.commerce.product.application.port.MemberPort;
import shop.dear.commerce.product.application.port.PresignedUrlGenerator;
import shop.dear.commerce.product.domain.constant.ProductCategory;
import shop.dear.commerce.product.domain.constant.ProductSaleType;
import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.commerce.product.domain.constant.UploadFileType;
import shop.dear.commerce.product.domain.model.Price;
import shop.dear.commerce.product.domain.model.Product;
import shop.dear.commerce.product.domain.model.ProductImage;
import shop.dear.commerce.product.domain.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Transactional
@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private FakeProductEventPublisher fakeProductEventPublisher;

    @BeforeEach
    void setUp() {
        final MemberPort fakeMemberPort = new FakeMemberPort();
        final FakeOfferPort fakeOfferPort = new FakeOfferPort();
        final PresignedUrlGenerator fakePresignedUrlGenerator = new FakePresignedUrlGenerator();
        fakeProductEventPublisher = new FakeProductEventPublisher();

        productService = new ProductService(
            productRepository,
            fakeMemberPort,
            fakeOfferPort,
            fakeProductEventPublisher,
            fakePresignedUrlGenerator
        );
    }

    private Product createProduct(final Long memberId) {
        return Product.create(
            memberId,
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

    @DisplayName("유효한 파일 정보가 들어오면 각 정보에 대한 Presigned Url을 반환한다.")
    @Test
    void givenFileInfo_whenGeneratePresignedUrls_thenReturnUrls() {
        //Given
        final Long memberId = 1L;
        final GeneratePresignedUrlsCommand command = new GeneratePresignedUrlsCommand(List.of(
            new GeneratePresignedUrlsCommand.FileInfoCommand(1, UploadFileType.PNG),
            new GeneratePresignedUrlsCommand.FileInfoCommand(2, UploadFileType.JPEG)
        ));

        //When
        final List<PresignedUrlInfoDto> result = productService.generatePresignedUrls(memberId, command);

        //Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).sortOrder()).isEqualTo(1);
        assertThat(result.get(0).presignedUrl()).containsIgnoringCase("png");
    }

    @DisplayName("유효한 값(sellerId, CreateProductCommand)이 들어오면 새로운 상품을 등록하고 각 데이터를 DB에 저장한다.")
    @Test
    void givenCreateProductCommand_whenCreateProduct_thenSaveDB() {
        //Given
        final Long sellerId = 1L;
        final CreateProductCommand command = new CreateProductCommand(
            ProductSaleType.OFFER,
            List.of(
                new CreateProductCommand.ProductImageContentCommand(1, "1.png", "1번 이야기"),
                new CreateProductCommand.ProductImageContentCommand(2, "2.png", "2번 이야기")
            ),
            "testBrand",
            "testName",
            BigDecimal.valueOf(120000),
            "testModelNumber-001",
            ProductCategory.SNEAKERS,
            LocalDate.now(),
            "Test Description"
        );

        //When
        productService.createProduct(sellerId, command);

        //Then
        final List<Product> products = productRepository.findAll();
        assertThat(products).hasSize(1);

        final Product savedProduct = products.get(0);

        assertAll(
            () -> assertThat(savedProduct.getSellerId()).isEqualTo(sellerId),
            () -> assertThat(savedProduct.getName()).isEqualTo(command.name()),
            () -> assertThat(savedProduct.getBrand()).isEqualTo(command.brand()),
            () -> assertThat(savedProduct.getModelNumber()).isEqualTo(command.modelNumber()),
            () -> assertThat(savedProduct.getCategory()).isEqualTo(ProductCategory.SNEAKERS),
            () -> assertThat(savedProduct.getReleaseDate()).isEqualTo(command.releaseDate()),
            () -> assertThat(savedProduct.getPrice()).isEqualTo(Price.from(command.price())),
            () -> assertThat(savedProduct.getSaleType()).isEqualTo(command.saleType()),
            () -> assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.PREPARING),
            () -> assertThat(savedProduct.getViewCount()).isEqualTo(0L),
            () -> assertThat(savedProduct.getDescription()).isEqualTo(command.description())
        );

        assertThat(savedProduct.getImages()).hasSize(2);
        assertThat(fakeProductEventPublisher.getEvents()).hasSize(1);
    }

    @DisplayName("유효한 값(sellerId, CreateProductCommand)이 들어오면 기존 상품을 수정하고 각 데이터를 DB에 저장한다.")
    @Test
    void givenUpdateProductCommand_whenUpdateProduct_thenSaveDB() {
        //Given
        final Long sellerId = 1L;
        final UpdateProductCommand command = new UpdateProductCommand(
            List.of(
                new UpdateProductCommand.ProductImageContentCommand(1, "1.png", "1번 이야기"),
                new UpdateProductCommand.ProductImageContentCommand(2, "2.png", "2번 이야기")
            ),
            "testBrand",
            "testName",
            BigDecimal.valueOf(120000),
            "testModelNumber-001",
            ProductCategory.SNEAKERS,
            LocalDate.now(),
            "Test Description"
        );

        final Product previousProduct = createProduct(sellerId);
        final Product previousSavedProduct = productRepository.save(previousProduct);

        //When
        productService.updateProduct(sellerId, previousSavedProduct.getId(), command);

        //Then
        final List<Product> products = productRepository.findAll();
        assertThat(products).hasSize(1);

        final Product savedProduct = products.get(0);

        assertAll(
            () -> assertThat(savedProduct.getSellerId()).isEqualTo(sellerId),
            () -> assertThat(savedProduct.getName()).isEqualTo(command.name()),
            () -> assertThat(savedProduct.getBrand()).isEqualTo(command.brand()),
            () -> assertThat(savedProduct.getModelNumber()).isEqualTo(command.modelNumber()),
            () -> assertThat(savedProduct.getCategory()).isEqualTo(ProductCategory.SNEAKERS),
            () -> assertThat(savedProduct.getReleaseDate()).isEqualTo(command.releaseDate()),
            () -> assertThat(savedProduct.getPrice()).isEqualTo(Price.from(command.price())),
            () -> assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.PREPARING),
            () -> assertThat(savedProduct.getViewCount()).isEqualTo(0L),
            () -> assertThat(savedProduct.getDescription()).isEqualTo(command.description())
        );

        assertThat(savedProduct.getImages()).hasSize(2);
        assertThat(fakeProductEventPublisher.getEvents()).hasSize(1);
    }

    @DisplayName("유효한 값(sellerId, productId)이 들어오면 해당 상품을 DB에서 삭제한다.")
    @Test
    void givenSellerIdAndProductId_whenDeleteProduct_thenSuccess() {
        //Given
        final Long sellerId = 1L;
        final Product product = createProduct(1L);
        final Product savedProduct = productRepository.save(product);

        //When
        productService.deleteProduct(savedProduct.getSellerId(), savedProduct.getId());

        //Then
        final List<Product> products = productRepository.findAll();
        assertThat(products.size()).isEqualTo(0);
    }

    @DisplayName("유효한 값(sellerId)이 들어오면 해당 사용자가 등록한 판매 예정 및 판매중인 상품이 존재하는지 여부를 반환한다.")
    @Test
    void givenSellerId_whenGetMemberProductExists_thenReturnExists() {
        //Given
        final Long sellerId1 = 1L;
        final Product product1 = createProduct(sellerId1);
        productRepository.save(product1);

        final Long sellerId2 = 2L;
        final Product product2 = createProduct(sellerId2);
        product2.changeStatusToOnSale();
        productRepository.save(product2);

        final Long sellerId3 = 3L;
        final Product product3 = createProduct(sellerId3);
        product3.changeStatusToSoldOut();
        productRepository.save(product3);

        //When
        final MemberProductExistsDto result1 = productService.getMemberProductExists(sellerId1);
        final MemberProductExistsDto result2 = productService.getMemberProductExists(sellerId2);
        final MemberProductExistsDto result3 = productService.getMemberProductExists(sellerId3);

        //Then
        assertThat(result1.exists()).isEqualTo(true);
        assertThat(result2.exists()).isEqualTo(true);
        assertThat(result3.exists()).isEqualTo(false);
    }

    @DisplayName("유효한 값(memberId, product ids)이 들어오면 해당 id에 맞는 상품을 반환한다.")
    @Test
    void givenMemberIdAndProductIds_whenGetScrapProducts_thenReturnProducts() {
        //Given
        final Long memberId = 1L;

        final Product product1 = createProduct(memberId);
        product1.addImage("test1.png", 1);
        final Product savedProduct1 = productRepository.save(product1);

        final Product product2 = createProduct(memberId);
        product2.addImage("test2.png", 1);
        final Product savedProduct2 = productRepository.save(product2);

        final List<Long> ids = List.of(savedProduct1.getId(), savedProduct2.getId());
        final GetScrapProductCommand command = new GetScrapProductCommand(ids);

        //When
        final List<ScrapProductInfoDto> result = productService.getScrapProducts(memberId, command);

        //Then
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).id()).isEqualTo(savedProduct1.getId());
        assertThat(result.get(1).id()).isEqualTo(savedProduct2.getId());
    }

    @DisplayName("유효한 값(memberId, productId)이 들어오면 해당 id에 맞는 상품의 상세정보를 조회한다.")
    @Test
    void givenMemberIdAndProductId_whenGetProductDetail_thenReturnProductDetail() {
        //Given
        final Long memberId = 1L;

        final Long sellerId = 2L;
        final String name = "Dear Sneakers";
        final String brand = "Dear";
        final BigDecimal priceValue = BigDecimal.valueOf(150000);
        final String modelNumber = "DEAR-001";
        final ProductCategory category = ProductCategory.SNEAKERS;
        final LocalDate releaseDate = LocalDate.of(2026, 1, 1);
        final ProductSaleType saleType = ProductSaleType.OFFER;
        final String description = "상품 상세 설명입니다.";

        final int sortOrder = 1;
        final String url = "test1.png";
        final String story = "content1";

        final Product product = Product.create(
            sellerId,
            name,
            brand,
            modelNumber,
            category,
            releaseDate,
            Price.from(priceValue),
            saleType,
            description
        );
        final ProductImage image = product.addImage(url, sortOrder);
        image.addStory(story);

        product.changeStatusToOnSale();

        final Product savedProduct = productRepository.save(product);

        //When
        final GetProductDetailDto result = productService.getProductDetail(memberId, product.getId());

        //Then
        assertThat(result.sellerId()).isEqualTo(sellerId);
        assertThat(result.name()).isEqualTo(name);
        assertThat(result.brand()).isEqualTo(brand);
        assertThat(result.price()).isEqualByComparingTo(priceValue);
        assertThat(result.modelNumber()).isEqualTo(modelNumber);
        assertThat(result.category()).isEqualTo(category.toString());
        assertThat(result.releaseDate()).isEqualTo(releaseDate);
        assertThat(result.viewCount()).isEqualTo(1);
        assertThat(result.description()).isEqualTo(description);
        assertThat(result.insertedAt()).isEqualTo(savedProduct.getInsertedAt());

        assertThat(result.images().size()).isEqualTo(1);
        assertThat(result.images().getFirst().sortOrder()).isEqualTo(sortOrder);
        assertThat(result.images().getFirst().url()).isEqualTo(url);
        assertThat(result.images().getFirst().story()).isEqualTo(story);
    }

    @DisplayName("유효한 값(sellerId)이 들어오면 해당 판매자가 등록한 상품목록을 조회한다.")
    @Test
    void givenSellerId_whenGetSellerProducts_thenReturnProducts() {
        //Given
        final Long sellerId = 1L;
        final Product product = createProduct(sellerId);
        product.addImage("test1.png", 1);
        final Product savedProduct = productRepository.save(product);

        //When
        final List<GetSellerProductDto> result = productService.getSellerProducts(sellerId);

        //Then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.getFirst().id()).isEqualTo(savedProduct.getId());
        assertThat(result.getFirst().status()).isEqualTo(savedProduct.getStatus().toString());
        assertThat(result.getFirst().url()).isEqualTo(savedProduct.getImages().getFirst().getUrl());
        assertThat(result.getFirst().name()).isEqualTo(savedProduct.getName());
        assertThat(result.getFirst().brand()).isEqualTo(savedProduct.getBrand());
        assertThat(result.getFirst().price()).isEqualTo(savedProduct.getPrice().getValue());
        assertThat(result.getFirst().viewCount()).isEqualTo(savedProduct.getViewCount());
    }
}