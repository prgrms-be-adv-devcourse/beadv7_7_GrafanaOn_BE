package shop.dear.identity.auth.authorization.domain;

// 비회원은 회원이 아니므로 GUEST 같은 상태 저장 X
public enum Role {
    BUYER,
    SELLER
}
