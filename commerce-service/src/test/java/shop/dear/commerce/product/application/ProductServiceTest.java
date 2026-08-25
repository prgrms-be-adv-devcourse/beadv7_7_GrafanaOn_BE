package shop.dear.commerce.product.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.product.application.dto.GetProductDetailDto;
import shop.dear.commerce.product.application.dto.GetProductDto;
import shop.dear.commerce.product.application.dto.GetSellerProductDto;
import shop.dear.commerce.product.application.dto.MemberProductExistsDto;
import shop.dear.commerce.product.application.dto.PresignedUrlInfoDto;
import shop.dear.commerce.product.application.dto.ScrapProductInfoDto;
import shop.dear.commerce.product.application.dto.TradeProductDto;
import shop.dear.commerce.product.application.dto.command.CreateProductCommand;
import shop.dear.commerce.product.application.dto.command.GeneratePresignedUrlsCommand;
import shop.dear.commerce.product.application.dto.command.GetScrapProductCommand;
import shop.dear.commerce.product.application.dto.command.UpdateProductCommand;
import shop.dear.commerce.product.application.dto.external.PublishProductInfo;
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
import shop.dear.commerce.product.infrastructure.outbox.ProductOutboxAppender;
import shop.dear.common.pagination.PaginationRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static java.time.temporal.ChronoUnit.MICROS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;

@Transactional
@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private FakeProductEventPublisher fakeProductEventPublisher;
    @Autowired
    private ProductScheduler productScheduler;

    @Autowired
    private ProductOutboxAppender productOutboxAppender;

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
            fakePresignedUrlGenerator,
            productOutboxAppender
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

    private Product createProduct(final Long memberId, final ProductSaleType saleType) {
        return Product.create(
            memberId,
            "testName",
            "testBrand",
            "testModelNumber-001",
            ProductCategory.SNEAKERS,
            LocalDate.now(),
            Price.from(BigDecimal.valueOf(120000)),
            saleType,
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

        assertThat(products).isEmpty();
    }

    @DisplayName("유효한 값(sellerId)이 들어오면 해당 사용자가 등록한 판매 예정 및 판매중, 거래중인 상품이 존재하는지 여부를 반환한다.")
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
        product3.changeStatusToTrading();
        productRepository.save(product3);

        final Long sellerId4 = 4L;
        final Product product4 = createProduct(sellerId4);
        product4.changeStatusToSoldOut();
        productRepository.save(product4);

        //When
        final MemberProductExistsDto result1 = productService.getMemberProductExists(sellerId1);
        final MemberProductExistsDto result2 = productService.getMemberProductExists(sellerId2);
        final MemberProductExistsDto result3 = productService.getMemberProductExists(sellerId3);
        final MemberProductExistsDto result4 = productService.getMemberProductExists(sellerId4);

        //Then
        assertThat(result1.exists()).isEqualTo(true);
        assertThat(result2.exists()).isEqualTo(true);
        assertThat(result3.exists()).isEqualTo(true);
        assertThat(result4.exists()).isEqualTo(false);
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
        final Long memberId = 2L;

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
        assertThat(result.insertedAt()).isCloseTo(savedProduct.getInsertedAt(), within(1, MICROS));

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
        final PaginationRequest paginationRequest = new PaginationRequest(1, 10, 2, 2);
        final Page<GetSellerProductDto> result = productService.getSellerProducts(sellerId, paginationRequest.toPageable());

        //Then
        assertThat(result.getContent().size()).isEqualTo(1);
        assertThat(result.getContent().getFirst().id()).isEqualTo(savedProduct.getId());
        assertThat(result.getContent().getFirst().status()).isEqualTo(savedProduct.getStatus().toString());
        assertThat(result.getContent().getFirst().url()).isEqualTo(savedProduct.getImages().getFirst().getUrl());
        assertThat(result.getContent().getFirst().name()).isEqualTo(savedProduct.getName());
        assertThat(result.getContent().getFirst().brand()).isEqualTo(savedProduct.getBrand());
        assertThat(result.getContent().getFirst().price()).isEqualTo(savedProduct.getPrice().getValue());
        assertThat(result.getContent().getFirst().viewCount()).isEqualTo(savedProduct.getViewCount());
    }

    @DisplayName("삭제된 상품이면 판매자 상품 목록에 조회되지 않는다.")
    @Test
    void givenSellerId_whenGetSellerProductIsAlreadyDeleted_thenReturnProducts() {
        //Given
        final Long sellerId = 1L;
        final Product product = createProduct(sellerId);
        product.addImage("test1.png", 1);
        final Product savedProduct = productRepository.save(product);

        productService.deleteProduct(sellerId, savedProduct.getId());

        //When
        final PaginationRequest paginationRequest = new PaginationRequest(1, 10, 2, 2);
        final Page<GetSellerProductDto> result = productService.getSellerProducts(sellerId, paginationRequest.toPageable());

        //Then
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @DisplayName("판매자가 등록한 상품목록 조회 시 페이징 처리한다.")
    @Test
    void givenPageInfo_whenGetSellerProducts_thenReturnPagingProducts() {
        //Given
        final Long sellerId = 1L;

        final Product product1 = createProduct(sellerId);
        product1.addImage("test1.png", 1);
        final Product savedProduct1 = productRepository.save(product1);

        final Product product2 = createProduct(sellerId);
        product2.addImage("test2.png", 1);
        final Product savedProduct2 = productRepository.save(product2);

        final Product product3 = createProduct(sellerId);
        product3.addImage("test2.png", 3);
        final Product savedProduct3 = productRepository.save(product3);

        //When
        final PaginationRequest paginationRequest = new PaginationRequest(1, 10, 2, 2);
        final Page<GetSellerProductDto> result = productService.getSellerProducts(sellerId, paginationRequest.toPageable());

        //Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getNumber()).isEqualTo(0);
    }

    @DisplayName("판매자가 등록한 상품목록 조회에서 마지막 페이지 조회 시 남은 데이터를 반환한다.")
    @Test
    void givenPageInfo_whenGetSellerProducts_thenReturnRemainProducts() {
        // Given
        final Long sellerId = 1L;

        final Product product1 = createProduct(sellerId, ProductSaleType.OFFER);  // saleType = OFFER, status = PREPARING
        product1.addImage("test1.png", 1);
        final Product savedProduct1 = productRepository.save(product1);

        final Product product2 = createProduct(sellerId, ProductSaleType.IMMEDIATE); // saleType = IMMEDIATE, status = PREPARING
        product2.addImage("test2.png", 1);
        final Product savedProduct2 = productRepository.save(product2);

        final Product product3 = createProduct(sellerId, ProductSaleType.OFFER);  // saleType = OFFER, status = ON_SALE
        product3.addImage("test3.png", 1);
        final Product savedProduct3 = productRepository.save(product3);

        // When
        final PaginationRequest paginationRequest = new PaginationRequest(2, 10, 2, 2);
        final Page<GetSellerProductDto> result = productService.getSellerProducts(sellerId, paginationRequest.toPageable());

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isTrue();
        assertThat(result.hasNext()).isFalse();
    }

    @DisplayName("유효한 값(saleType, status)이 들어오면 해당 상태에 맞는 상품들을 조회한다.")
    @Test
    void givenSaleTypeAndStatus_whenGetAllProduct_thenReturnProducts() {
        // Given
        final Long sellerId = 1L;

        final Product product1 = createProduct(sellerId, ProductSaleType.OFFER);  // saleType = OFFER, status = PREPARING
        product1.addImage("test1.png", 1);
        final Product savedProduct1 = productRepository.save(product1);

        final Product product2 = createProduct(sellerId, ProductSaleType.IMMEDIATE); // saleType = IMMEDIATE, status = PREPARING
        product2.addImage("test2.png", 1);
        final Product savedProduct2 = productRepository.save(product2);

        final Product product3 = createProduct(sellerId, ProductSaleType.OFFER);  // saleType = OFFER, status = ON_SALE
        product3.addImage("test3.png", 1);
        product3.changeStatusToOnSale();
        final Product savedProduct3 = productRepository.save(product3);

        final ProductSaleType targetSaleType = ProductSaleType.OFFER;
        final ProductStatus targetStatus = ProductStatus.PREPARING;
        final LocalDate date = LocalDate.now();
        final ProductCategory category = ProductCategory.SNEAKERS;

        // When
        final PaginationRequest paginationRequest = new PaginationRequest(1, 10, 2, 2);
        final Page<GetProductDto> result = productService.getAllProduct(targetSaleType, targetStatus, date, category, paginationRequest.toPageable());

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(savedProduct1.getId());
        assertThat(result.getContent().get(0).saleType()).isEqualTo(targetSaleType.toString());
        assertThat(result.getContent().get(0).status()).isEqualTo(targetStatus.toString());
    }

    @DisplayName("파라미터(saleType, status, createdAt)가 null로 주어지면 전체 상품 목록을 조회한다.")
    @Test
    void givenNullParams_whenGetAllProduct_thenReturnAllProducts() {
        // Given
        final Long sellerId = 1L;

        final Product product1 = createProduct(sellerId, ProductSaleType.OFFER);
        product1.addImage("test1.png", 1);

        final Product product2 = createProduct(sellerId, ProductSaleType.IMMEDIATE);
        product2.addImage("test2.png", 1);

        productRepository.save(product1);
        productRepository.save(product2);

        // When
        final PaginationRequest paginationRequest = new PaginationRequest(1, 10, 2, 2);
        final Page<GetProductDto> result = productService.getAllProduct(null, null, null, null, paginationRequest.toPageable());

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @DisplayName("조건에 해당하는 상품이 없으면 빈 리스트를 반환한다.")
    @Test
    void givenNonMatchingFilter_whenGetAllProduct_thenReturnEmptyList() {
        // Given
        final Long sellerId = 1L;
        final Product product = createProduct(sellerId, ProductSaleType.OFFER); // status = PREPARING
        productRepository.save(product);

        // When
        final PaginationRequest paginationRequest = new PaginationRequest(1, 10, 2, 2);
        final Page<GetProductDto> result = productService.getAllProduct(
            ProductSaleType.IMMEDIATE,
            ProductStatus.SOLD_OUT,
            null,
            ProductCategory.BOOTS,
            paginationRequest.toPageable()
        );

        // Then
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @DisplayName("삭제된 상품이면 상품 목록에 조회되지 않는다.")
    @Test
    void givenProductDelete_whenGetAllProduct_thenReturnEmptyList() {
        // Given
        final Long sellerId = 1L;
        final Product product = createProduct(sellerId, ProductSaleType.OFFER); // status = PREPARING
        final Product savedProduct = productRepository.save(product);

        productService.deleteProduct(sellerId, savedProduct.getId());

        // When
        final PaginationRequest paginationRequest = new PaginationRequest(1, 10, 2, 2);
        final Page<GetProductDto> result = productService.getAllProduct(
            null,
            null,
            null,
            null,
            paginationRequest.toPageable()
        );

        // Then
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @DisplayName("상품 목록 조회 시 페이징 처리한다.")
    @Test
    void givenPageInfo_whenGetAllProduct_thenReturnPagingProducts() {
        // Given
        final Long sellerId = 1L;

        final Product product1 = createProduct(sellerId, ProductSaleType.OFFER);  // saleType = OFFER, status = PREPARING
        product1.addImage("test1.png", 1);
        final Product savedProduct1 = productRepository.save(product1);

        final Product product2 = createProduct(sellerId, ProductSaleType.IMMEDIATE); // saleType = IMMEDIATE, status = PREPARING
        product2.addImage("test2.png", 1);
        final Product savedProduct2 = productRepository.save(product2);

        final Product product3 = createProduct(sellerId, ProductSaleType.OFFER);  // saleType = OFFER, status = ON_SALE
        product3.addImage("test3.png", 1);
        product3.changeStatusToOnSale();
        final Product savedProduct3 = productRepository.save(product3);

        // When
        final PaginationRequest paginationRequest = new PaginationRequest(1, 10, 2, 2);
        final Page<GetProductDto> result = productService.getAllProduct(null, null, null, null, paginationRequest.toPageable());

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getNumber()).isEqualTo(0);
    }

    @DisplayName("상품 목록 조회에서 마지막 페이지 조회 시 남은 데이터를 반환한다.")
    @Test
    void givenPageInfo_whenGetAllProduct_thenReturnRemainProducts() {
        // Given
        final Long sellerId = 1L;

        final Product product1 = createProduct(sellerId, ProductSaleType.OFFER);  // saleType = OFFER, status = PREPARING
        product1.addImage("test1.png", 1);
        final Product savedProduct1 = productRepository.save(product1);

        final Product product2 = createProduct(sellerId, ProductSaleType.IMMEDIATE); // saleType = IMMEDIATE, status = PREPARING
        product2.addImage("test2.png", 1);
        final Product savedProduct2 = productRepository.save(product2);

        final Product product3 = createProduct(sellerId, ProductSaleType.OFFER);  // saleType = OFFER, status = ON_SALE
        product3.addImage("test3.png", 1);
        product3.changeStatusToOnSale();
        final Product savedProduct3 = productRepository.save(product3);

        // When
        final PaginationRequest paginationRequest = new PaginationRequest(2, 10, 2, 2);
        final Page<GetProductDto> result = productService.getAllProduct(null, null, null, null, paginationRequest.toPageable());

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isTrue();
        assertThat(result.hasNext()).isFalse();
    }

    @DisplayName("등록된 상품의 상태를 판매중으로 변경한다.")
    @Test
    void whenPublishDailyProducts_thenSuccess() {
        //Given
        final Long sellerId = 1L;
        final Product product = createProduct(sellerId);
        final Product savedProduct = productRepository.save(product);

        //When
        final PublishProductInfo info = productScheduler.publishDailyProducts();

        final Product updatedProduct = productRepository.findById(savedProduct.getId());

        //Then
        assertThat(info.count()).isEqualTo(1);
        assertThat(updatedProduct.getStatus().toString()).isEqualTo("ON_SALE");
    }

    @DisplayName("판매 완료된 상품의 상태를 변경한다.")
    @Test
    void givenProductId_whenCompleteProductSale_thenChangeProductStatus() {
        //Given
        final Long member1 = 1L;
        final Product product1 = createProduct(member1);
        product1.changeStatusToOnSale();
        final Product savedProduct1 = productRepository.save(product1);

        final Long member2 = 1L;
        final Product product2 = createProduct(member2);
        product2.changeStatusToSoldOut();
        final Product savedProduct2 = productRepository.save(product2);

        //When
        productService.completeProductSale(product1.getId());
        productService.completeProductSale(product2.getId());

        //Then
        assertThat(savedProduct1.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
        assertThat(savedProduct2.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    @DisplayName("판매중인 상품의 상태를 거래중으로 변경한다.")
    @Test
    void givenProductId_whenTradeProduct_thenChangeStatusTrade() {
        //Given
        final Long memberId = 1L;
        final Product product = createProduct(memberId);
        product.changeStatusToOnSale();

        final Product savedProduct = productRepository.save(product);

        //When
        final TradeProductDto result = productService.tradeProduct(memberId, savedProduct.getId());

        //Then
        assertThat(result.isChanged()).isTrue();
    }

    @DisplayName("구매 취소 시 상품의 상태를 판매중으로 변경한다.")
    @Test
    void givenProductId_whenCanceledPurchase_thenChangeProductStatusIsOnSale() {
        //Given
        final Long memberId = 1L;
        final Product product = createProduct(memberId);
        product.changeStatusToTrading();
        final Product savedProduct = productRepository.save(product);

        //When
        productService.canceledPurchase(savedProduct.getId());

        //Then
        assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }
}