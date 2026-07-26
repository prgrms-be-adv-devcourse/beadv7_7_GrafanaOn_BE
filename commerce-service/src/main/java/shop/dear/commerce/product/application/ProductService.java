package shop.dear.commerce.product.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.product.application.dto.MemberProductExistsDto;
import shop.dear.commerce.product.application.dto.PresignedUrlInfo;
import shop.dear.commerce.product.application.dto.command.CreateProductCommand;
import shop.dear.commerce.product.application.dto.command.UpdateProductCommand;
import shop.dear.commerce.product.application.dto.external.ExistsMember;
import shop.dear.commerce.product.application.dto.external.ExistsOffer;
import shop.dear.commerce.product.application.dto.external.GeneratePresignedUrlsCommand;
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

    public List<PresignedUrlInfo> generatePresignedUrls(final Long memberId, final GeneratePresignedUrlsCommand generatePresignedUrlsCommand) {
        validateMember(memberId);

        return generatePresignedUrlsCommand.files().stream()
            .map(imageInfo -> new PresignedUrlInfo(
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
            savedProduct.getCategory().toString(),
            savedProduct.getReleaseDate(),
            savedProduct.getPrice().getValue(),
            savedProduct.getSaleType().toString(),
            savedProduct.getViewCount(),
            savedProduct.getDescription(),
            fullStory.toString()
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
            savedProduct.getCategory().toString(),
            savedProduct.getReleaseDate(),
            savedProduct.getPrice().getValue(),
            savedProduct.getSaleType().toString(),
            savedProduct.getViewCount(),
            savedProduct.getDescription(),
            fullStory.toString()
        ));
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

    public void deleteProduct(final Long sellerId, final Long productId) {
        validateMember(sellerId);
        validateSeller(sellerId);

        final Product product = productRepository.findById(productId);
        product.validateOwner(sellerId);
        validateProductDeletable(product);

        productRepository.delete(product);

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
}
