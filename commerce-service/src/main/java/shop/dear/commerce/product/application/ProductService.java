package shop.dear.commerce.product.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.product.application.dto.GetProductDetailDto;
import shop.dear.commerce.product.application.dto.GetProductDto;
import shop.dear.commerce.product.application.dto.GetSellerProductDto;
import shop.dear.commerce.product.application.dto.MemberProductExistsDto;
import shop.dear.commerce.product.application.dto.PresignedUrlInfoDto;
import shop.dear.commerce.product.application.dto.ScrapProductInfoDto;
import shop.dear.commerce.product.application.dto.command.CreateProductCommand;
import shop.dear.commerce.product.application.dto.command.GeneratePresignedUrlsCommand;
import shop.dear.commerce.product.application.dto.command.GetScrapProductCommand;
import shop.dear.commerce.product.application.dto.command.UpdateProductCommand;
import shop.dear.commerce.product.application.dto.external.ExistsMember;
import shop.dear.commerce.product.application.dto.external.ExistsOffer;
import shop.dear.commerce.product.application.port.MemberPort;
import shop.dear.commerce.product.application.port.OfferPort;
import shop.dear.commerce.product.application.port.PresignedUrlGenerator;
import shop.dear.commerce.product.application.port.ProductEventPublisher;
import shop.dear.commerce.product.domain.constant.ProductSaleType;
import shop.dear.commerce.product.domain.constant.ProductStatus;
import shop.dear.commerce.product.domain.exception.ProductErrorCode;
import shop.dear.commerce.product.domain.model.Price;
import shop.dear.commerce.product.domain.model.Product;
import shop.dear.commerce.product.domain.model.ProductImage;
import shop.dear.commerce.product.domain.repository.ProductRepository;
import shop.dear.common.event.product.ProductChangedEvent;
import shop.dear.common.event.product.ProductDeletedEvent;
import shop.dear.common.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final MemberPort memberPort;
    private final OfferPort offerPort;
    private final ProductEventPublisher productEventPublisher;
    private final PresignedUrlGenerator presignedUrlGenerator;

    public List<PresignedUrlInfoDto> generatePresignedUrls(final Long memberId, final GeneratePresignedUrlsCommand generatePresignedUrlsCommand) {
        validateMember(memberId);

        return generatePresignedUrlsCommand.files().stream()
            .map(imageInfo -> new PresignedUrlInfoDto(
                imageInfo.sortOrder(),
                presignedUrlGenerator.generate(imageInfo.sortOrder(), imageInfo.uploadFileType())
                ))
            .toList();
    }

    private void validateMember(final Long memberId) {
        final ExistsMember existsMember = memberPort.existsMember(memberId);

        if (!existsMember.exists()) {
            throw new BusinessException(ProductErrorCode.INVALID_MEMBER);
        }
    }

    @Transactional
    public void createProduct(final Long sellerId, final CreateProductCommand command) {
        validateMember(sellerId);
        validateSeller(sellerId);

        final Product product = Product.create(
            sellerId,
            command.name(),
            command.brand(),
            command.modelNumber(),
            command.category(),
            command.releaseDate(),
            Price.from(command.price()),
            command.saleType(),
            command.description()
        );

        final StringBuilder fullStory = new StringBuilder();

        for (final CreateProductCommand.ProductImageContentCommand content : command.productImageContents()) {
            final ProductImage image = product.addImage(content.url(), content.sortOrder());

            if (content.story() != null) {
                image.addStory(content.story());
                fullStory.append(content.story()).append(" ");
            }
        }

        final Product savedProduct = productRepository.save(product);

        productEventPublisher.publish(new ProductChangedEvent(
                savedProduct.getId(),
                savedProduct.getName(),
                savedProduct.getModelNumber(),
                savedProduct.getCategory().name(),
                savedProduct.getReleaseDate(),
                savedProduct.getPrice().getValue(),
                savedProduct.getSaleType().name(),
                savedProduct.getStatus().name(),
                savedProduct.getViewCount(),
                savedProduct.getDescription(),
                fullStory.toString().trim(),
                savedProduct.getInsertedAt()
        ));
    }

    private void validateSeller(final Long memberId) {
        if (!memberPort.isSeller(memberId).isSeller()) {
            throw new BusinessException(ProductErrorCode.REQUIRED_SELLER_ROLE);
        }
    }

    @Transactional
    public void updateProduct(final Long sellerId, final Long productId, final UpdateProductCommand command) {
        validateMember(sellerId);
        validateSeller(sellerId);

        final Product originalProduct = productRepository.findById(productId);
        validateDeleted(originalProduct);
        originalProduct.validateOwner(sellerId);
        validateProductUpdatable(originalProduct);

        final Product updatedProduct = originalProduct.update(
            command.name(),
            command.brand(),
            command.modelNumber(),
            command.category(),
            command.releaseDate(),
            Price.from(command.price()),
            command.description()
        );

        final StringBuilder fullStory = new StringBuilder();

        for (final UpdateProductCommand.ProductImageContentCommand content : command.productImageContents()) {
            final ProductImage image = updatedProduct.addImage(content.url(), content.sortOrder());

            if (content.story() != null) {
                image.addStory(content.story());
                fullStory.append(content.story()).append(" ");
            }
        }

        final Product savedProduct = productRepository.save(updatedProduct);

        productEventPublisher.publish(new ProductChangedEvent(
                savedProduct.getId(),
                savedProduct.getName(),
                savedProduct.getModelNumber(),
                savedProduct.getCategory().name(),
                savedProduct.getReleaseDate(),
                savedProduct.getPrice().getValue(),
                savedProduct.getSaleType().name(),
                savedProduct.getStatus().name(),
                savedProduct.getViewCount(),
                savedProduct.getDescription(),
                fullStory.toString().trim(),
                savedProduct.getInsertedAt()
        ));
    }

    private void validateDeleted(final Product product) {
        if (product.isDeleted()) {
            throw new BusinessException(ProductErrorCode.INVALID_PRODUCT);
        }
    }

    private void validateProductUpdatable(final Product originalProduct) {
        if (!originalProduct.isUpdatable()) {
            throw new BusinessException(ProductErrorCode.INVALID_PRODUCT_STATUS_FOR_UPDATE);
        }

        if (originalProduct.getSaleType() == ProductSaleType.OFFER) {
            final ExistsOffer existsOffer = offerPort.existsOffer(originalProduct.getId());

            if (existsOffer.exists()) {
                throw new BusinessException(ProductErrorCode.INVALID_PRODUCT_STATUS_FOR_UPDATE);
            }
        }
    }

    @Transactional
    public void deleteProduct(final Long sellerId, final Long productId) {
        validateMember(sellerId);
        validateSeller(sellerId);

        final Product product = productRepository.findById(productId);
        validateDeleted(product);
        product.validateOwner(sellerId);
        validateProductDeletable(product);

        product.delete();

        productEventPublisher.publish(new ProductDeletedEvent(productId));
    }

    private void validateProductDeletable(final Product product) {
        if (!product.isDeletable()) {
            throw new BusinessException(ProductErrorCode.INVALID_PRODUCT_STATUS_FOR_DELETE);
        }
    }

    public MemberProductExistsDto getMemberProductExists(final Long sellerId) {
        validateMember(sellerId);
        validateSeller(sellerId);

        final List<ProductStatus> statuses = List.of(ProductStatus.PREPARING, ProductStatus.ON_SALE);
        final boolean exists = productRepository.existsBySellerIdAndStatusIn(sellerId, statuses);

        return new MemberProductExistsDto(exists);
    }

    public List<ScrapProductInfoDto> getScrapProducts(final Long memberId, final GetScrapProductCommand command) {
        validateMember(memberId);

        final List<Product> scrapProducts = productRepository.findAllByIdIn(command.ids());

        return scrapProducts.stream()
            .map(ScrapProductInfoDto::from)
            .toList();
    }

    @Transactional
    public GetProductDetailDto getProductDetail(final Long memberId, final Long productId) {
        validateMember(memberId);
        validateProduct(productId);

        productRepository.increaseViewCount(productId);
        final Product product = productRepository.findById(productId);
        validateDeleted(product);

        validateProductVisible(product);

        return GetProductDetailDto.of(product);
    }

    private void validateProduct(final Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(ProductErrorCode.INVALID_PRODUCT);
        }
    }

    private void validateProductVisible(final Product product) {
        if (!product.isVisible()) {
            throw new BusinessException(ProductErrorCode.INVALID_PRODUCT_VISIBLE);
        }
    }

    public GetProductDetailDto getProductDetailToInternal(final Long memberId, final Long productId) {
        validateMember(memberId);

        final Product product = productRepository.findById(productId);

        validateDeleted(product);
        validateProductVisible(product);

        return GetProductDetailDto.of(product);
    }

    public List<GetSellerProductDto> getSellerProducts(final Long sellerId) {
        validateMember(sellerId);
        validateSeller(sellerId);

        final List<Product> products = productRepository.findAllBySellerIdAndDeletedAtIsNull(sellerId);

        return products.stream()
            .map(GetSellerProductDto::of)
            .toList();
    }

    public List<GetProductDto> getAllProduct(final ProductSaleType saleType, final ProductStatus status, final LocalDate createdAt) {
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (createdAt != null) {
            startDate = createdAt.atStartOfDay();
            endDate = createdAt.atTime(LocalTime.MAX);
        }

        final List<Product> products = productRepository.findAllBySaleTypeAndStatusAndCreatedAtAndDeletedAtIsNull(saleType, status, startDate, endDate);

        return products.stream()
            .map(GetProductDto::of)
            .toList();
    }

    @Transactional
    public void changeProductStatus(final Long productId) {
        final Product product = productRepository.findById(productId);

        if (product.getStatus() == ProductStatus.SOLD_OUT) {
            return;
        }

        product.changeStatusToSoldOut();
    }
}
