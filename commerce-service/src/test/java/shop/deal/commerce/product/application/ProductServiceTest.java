package shop.deal.commerce.product.application;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import shop.deal.commerce.product.application.dto.PresignedUrlInfo;
import shop.deal.commerce.product.application.dto.external.GeneratePresignedUrlsCommand;
import shop.deal.commerce.product.application.fake.FakeMemberPort;
import shop.deal.commerce.product.application.fake.FakePresignedUrlGenerator;
import shop.deal.commerce.product.application.port.MemberPort;
import shop.deal.commerce.product.application.port.PresignedUrlGenerator;
import shop.deal.commerce.product.domain.repository.ProductRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        final MemberPort fakeMemberPort = new FakeMemberPort();
        final PresignedUrlGenerator fakePresignedUrlGenerator = new FakePresignedUrlGenerator();

        productService = new ProductService(
            productRepository,
            fakeMemberPort,
            fakePresignedUrlGenerator
        );
    }

    @DisplayName("유효한 파일 정보가 들어오면 각 정보에 대한 Presigned Url을 반환한다.")
    @Test
    void givenMemberIdAndFileInfo_whenGeneratePresignedUrls_thenReturnUrls() {
        //Given
        final Long memberId = 1L;
        final GeneratePresignedUrlsCommand command = new GeneratePresignedUrlsCommand(List.of(
            new GeneratePresignedUrlsCommand.FileInfoCommand(1, "PNG"),
            new GeneratePresignedUrlsCommand.FileInfoCommand(2, "JPG")
        ));

        //When
        final List<PresignedUrlInfo> result = productService.generatePresignedUrls(memberId, command);

        //Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).sortOrder()).isEqualTo(1);
        assertThat(result.get(0).presignedUrl()).containsIgnoringCase("PNG");
    }
}