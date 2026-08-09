package shop.dear.commerce.product.domain.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductCategory {

    SNEAKERS("스니커즈"),
    SPORTS_SHOES("스포츠화"),
    DRESS_SHOES("구두"),
    BOOTS("부츠/워커"),
    SANDALS_SLIDES("샌들/슬리퍼"),
    WINTER_SHOES("패딩/퍼 신발"),
    ;

    private final String description;
}
