package shop.dear.identity.member.application.port;

import shop.dear.identity.member.application.dto.external.ExistsProduct;

public interface ProductPort {

    // 어떤 회원의 상품인지 받는다.
    ExistsProduct existsProduct(Long memberId);
}
