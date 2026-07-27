package shop.dear.commerce.order.purchase.application.port;

public interface MemberPort {

    void validateMemberExists(final Long memberId);
}
