package shop.dear.commerce.cart.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.dear.commerce.cart.application.CartService;
import shop.dear.commerce.cart.infrastructure.event.CartItemAddRequestedEvent;
import shop.dear.common.event.order.FinishedOrderEvent;


@Slf4j
@Component
@RequiredArgsConstructor
public class CartEventHandler {

    private final CartService cartService;

    @TransactionalEventListener
    public void handle(final FinishedOrderEvent event) {
        log.info("[CartEventHandler] 주문 완료 이벤트 수신: orderType={}, productId={}",
                event.orderType(), event.productId());
        cartService.removeProductOnOrderFinished(event);
    }

    @EventListener
    public void handle(final CartItemAddRequestedEvent event) {
        log.info("[CartEventHandler] 장바구니 추가 요청 수신: memberId={}, productId={}",
                event.memberId(), event.productId());
        cartService.addCartItem(event.memberId(), event.productId());
    }

}