package shop.deal.commerce.product.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.deal.commerce.product.application.dto.PresignedUrlInfo;
import shop.deal.commerce.product.application.dto.external.GeneratePresignedUrlsCommand;
import shop.deal.commerce.product.application.dto.external.MemberProfile;
import shop.deal.commerce.product.application.port.MemberPort;
import shop.deal.commerce.product.application.port.PresignedUrlGenerator;
import shop.deal.commerce.product.domain.repository.ProductRepository;

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
}
