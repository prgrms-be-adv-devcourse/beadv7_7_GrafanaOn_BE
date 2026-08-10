package shop.dear.commerce.order.purchase.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseExpirationScheduler {

    private final PurchaseService purchaseService;

    @Scheduled(fixedDelay = 60_000)
    public void expireOverduePurchases() {
        purchaseService.expireOverduePurchases();
    }
}
