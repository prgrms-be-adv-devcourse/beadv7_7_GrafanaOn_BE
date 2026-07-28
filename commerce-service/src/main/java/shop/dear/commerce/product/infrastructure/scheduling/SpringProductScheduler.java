package shop.dear.commerce.product.infrastructure.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shop.dear.commerce.product.application.ProductScheduler;
import shop.dear.commerce.product.application.dto.external.PublishProductInfo;

@Slf4j
@RequiredArgsConstructor
@Component
public class SpringProductScheduler {

    private final ProductScheduler productScheduler;

    @Scheduled(cron = "0 0 20 * * *")
    public void runPublishJob() {
        final PublishProductInfo info = productScheduler.publishDailyProducts();
        log.info("상품 상태 변경 완료 (범위: {} ~ {}): 총 {}건", info.startTime(), info.endTime(), info.count());
    }
}
