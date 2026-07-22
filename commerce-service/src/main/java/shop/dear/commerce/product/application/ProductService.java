package shop.dear.commerce.product.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shop.dear.commerce.product.application.dto.PresignedUrlInfo;
import shop.dear.commerce.product.application.dto.command.CreateProductCommand;
import shop.dear.commerce.product.application.dto.external.GeneratePresignedUrlsCommand;
import shop.dear.commerce.product.application.dto.external.MemberProfile;
import shop.dear.commerce.product.application.port.MemberPort;
import shop.dear.commerce.product.application.port.PresignedUrlGenerator;
import shop.dear.commerce.product.domain.exception.ProductErrorCode;
import shop.dear.commerce.product.domain.model.Product;
import shop.dear.commerce.product.domain.model.ProductImage;
import shop.dear.commerce.product.domain.repository.ProductRepository;
import shop.dear.common.exception.BusinessException;

import java.util.List;

@RequiredArgsConstructor
@Transactional
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final MemberPort memberPort;
    private final PresignedUrlGenerator presignedUrlGenerator;

    public List<PresignedUrlInfo> generatePresignedUrls(final Long memberId, final GeneratePresignedUrlsCommand generatePresignedUrlsCommand) {
        final MemberProfile memberProfile = memberPort.getMemberProfile(memberId);

        return generatePresignedUrlsCommand.files().stream()
            .map(imageInfo -> new PresignedUrlInfo(
                imageInfo.sortOrder(),
                presignedUrlGenerator.generate(imageInfo.sortOrder(), imageInfo.uploadFileType())
                ))
            .toList();
    }

    public void createProduct(final Long sellerId, final CreateProductCommand command) {
        final MemberProfile memberProfile = memberPort.getMemberProfile(sellerId);
        validateSeller(sellerId);

        final Product product = Product.create(
            sellerId,
            command.name(),
            command.brand(),
            command.modelNumber(),
            command.category(),
            command.releaseDate(),
            command.price(),
            command.saleType(),
            command.description()
        );

        for (final CreateProductCommand.ProductImageContentCommand content : command.productImageContents()) {
            final ProductImage image = product.addImage(content.url(), content.sortOrder());

            if (content.story() != null) {
                image.addStory(content.story());
            }
        }

        productRepository.save(product);
    }

    private void validateSeller(final Long memberId) {
        if (!memberPort.isSeller(memberId).isSeller()) {
            throw new BusinessException(ProductErrorCode.SELLER_ROLE_REQUIRED);
        }
    }
}
