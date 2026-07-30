package shop.dear.commerce.cart.application;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shop.dear.commerce.cart.application.dto.CartItemDto;
import shop.dear.commerce.cart.application.dto.GetAllCartItemProductResponse;
import shop.dear.commerce.cart.application.port.CartProductPort;
import shop.dear.commerce.cart.application.port.dto.CartProductInfo;
import shop.dear.commerce.cart.domain.constant.CartItemStatus;
import shop.dear.commerce.cart.domain.exception.CartErrorCode;
import shop.dear.commerce.cart.domain.model.Cart;
import shop.dear.commerce.cart.domain.model.CartItem;
import shop.dear.commerce.cart.domain.repository.CartItemRepository;
import shop.dear.commerce.cart.domain.repository.CartRepository;
import shop.dear.common.event.order.FinishedOrderEvent;
import shop.dear.common.event.order.OrderType;
import shop.dear.common.exception.BusinessException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartProductPort cartProductPort;

    public GetAllCartItemProductResponse getCartItems(final Long memberId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(CartErrorCode.CART_NOT_FOUND));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId()).stream()
                .filter(item -> item.getStatus() == CartItemStatus.BEFORE_PAYMENT)
                .toList();
        if (cartItems.isEmpty()) {
            return GetAllCartItemProductResponse.of(cart.getId(), List.of());
        }

        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .toList();

        Map<Long, CartProductInfo> productMap = cartProductPort.getProducts(productIds).stream()
                .collect(Collectors.toMap(CartProductInfo::id, info -> info));

        List<CartItemDto> allCartItems = cartItems.stream()
                .map(cartItem -> {
                    CartProductInfo product = productMap.get(cartItem.getProductId());
                    return CartItemDto.of(
                            cartItem.getId(),
                            cartItem.getProductId(),
                            product.name(),
                            product.thumbnailUrl(),
                            product.price(),
                            cartItem.getStatus().name()
                    );
                })
                .toList();

        return GetAllCartItemProductResponse.of(cart.getId(), allCartItems);
    }
    @Transactional
    public void deleteAllCartItems(final Long memberId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(CartErrorCode.CART_NOT_FOUND));
        cartItemRepository.deleteByCartId(cart.getId());
    }

    @Transactional
    public void deleteSelectedCartItemsByUser(final Long memberId,final List<Long> productIds) {
        if(productIds == null || productIds.isEmpty()) {
            throw new BusinessException(CartErrorCode.INVALID_DELETE_REQUEST);
        }
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(CartErrorCode.CART_NOT_FOUND));
        cartItemRepository.deleteByCartIdAndProductIdIn(cart.getId(), productIds);
    }

    @Transactional
    public void removeProductOnOrderFinished(final FinishedOrderEvent event) {
        if(event.orderType() != OrderType.PURCHASE) {
            return;
        }
        cartRepository.findByMemberId(event.buyerId())
                .ifPresent(cart -> cartItemRepository.deleteByCartIdAndProductId(
                        cart.getId(),
                        event.productId()
                ));
    }

}