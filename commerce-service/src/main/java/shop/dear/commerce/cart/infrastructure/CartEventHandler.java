package shop.dear.commerce.cart.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import shop.dear.commerce.cart.application.CartService;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.common.event.order.FinishedOrderEvent;


@Slf4j
@Component
@RequiredArgsConstructor
public class CartEventHandler {

    private final CartService cartService;

}