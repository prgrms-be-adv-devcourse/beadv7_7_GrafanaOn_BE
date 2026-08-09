package shop.dear.commerce.product.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import shop.dear.commerce.product.application.dto.external.PublishProductInfo;
import shop.dear.commerce.product.application.port.PublishProduct;
import shop.dear.commerce.product.domain.repository.ProductRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProductScheduler implements PublishProduct {

    private final ProductRepository productRepository;

    @Override
    public PublishProductInfo publishDailyProducts() {
        final LocalDateTime endTime = LocalDate.now().atTime(20, 0, 0);  // 오늘 20:00:00
        final LocalDateTime startTime = endTime.minusDays(1); // 어제 20:00:00

        final int updateProductCount = productRepository.updateStatusToOnSale(startTime, endTime);
        log.info("상품 상태 변경 완료 (범위: {} ~ {}): 총 {}건", startTime, endTime, updateProductCount);

        return new PublishProductInfo(
            startTime,
            endTime,
            updateProductCount
        );
    }
}
