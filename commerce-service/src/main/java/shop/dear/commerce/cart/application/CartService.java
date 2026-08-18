package shop.dear.commerce.cart.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import shop.dear.common.type.OrderType;
import shop.dear.common.exception.BusinessException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId())
                .stream()
                .filter(item -> item.getStatus() == CartItemStatus.BEFORE_PAYMENT)
                .toList();

        if (cartItems.isEmpty()) {
            return GetAllCartItemProductResponse.of(cart.getId(), List.of());
        }

        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .distinct()
                .toList();

        log.info("[CartService] 장바구니 상품 ID 목록={}", productIds);

        List<CartProductInfo> products =
                cartProductPort.getProducts(productIds);

        log.info("[CartService] 상품 조회 결과={}", products);

        Map<Long, CartProductInfo> productMap =
             products.stream()
                     .collect(Collectors.toMap(
                             CartProductInfo::productId,
                             info -> info
                     ));

        List<CartItemDto> allCartItems = cartItems.stream()
                .map(cartItem -> {
                    CartProductInfo product = productMap.get(cartItem.getProductId());
                    if(product == null) {
                        log.error(
                                "[CartService] 상품 매핑 실패. productId={}, productMapKeys={}",
                                cartItem.getProductId(),
                                productMap.keySet()
                        );
                        throw new BusinessException(
                                CartErrorCode.PRODUCT_NOT_FOUND
                        );
                    }
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

        return GetAllCartItemProductResponse.of(
                cart.getId(),
                allCartItems
        );
    }

    @Transactional
    public void deleteAllCartItems(final Long memberId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(CartErrorCode.CART_NOT_FOUND));
        cartItemRepository.deleteByCartId(cart.getId());
    }

    @Transactional
    public void deleteSelectedCartItemsByUser(final Long memberId, final List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new BusinessException(CartErrorCode.INVALID_DELETE_REQUEST);
        }
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(CartErrorCode.CART_NOT_FOUND));
        cartItemRepository.deleteByCartIdAndProductIdIn(cart.getId(), productIds);
    }

    @Transactional
    public void removeProductOnOrderFinished(final FinishedOrderEvent event) {
        if (!OrderType.PURCHASE.name().equals(event.orderType())) {
            return;
        }
        cartRepository.findByMemberId(event.buyerId())
                .ifPresent(cart -> cartItemRepository.deleteByCartIdAndProductId(
                        cart.getId(),
                        event.productId()
                ));
    }

    @Transactional
    public void addCartItem(final Long memberId, Long productId) {
        final Cart cart = cartRepository.findByMemberId(memberId)
                .orElseGet(() -> cartRepository.save(Cart.create(memberId)));

        cartItemRepository.findByCartIdAndProductId(cart.getId(), productId).stream()
                .findFirst()
                .ifPresentOrElse(
                        item -> log.info("[CartService] 이미 장바구니에 존재하는 상품입니다. cartId={}, productId={}", cart.getId(), productId),
                        () -> cartItemRepository.save(CartItem.create(cart.getId(), productId))
                );
    }

}