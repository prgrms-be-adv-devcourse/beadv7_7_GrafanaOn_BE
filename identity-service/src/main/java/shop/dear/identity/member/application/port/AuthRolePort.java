package shop.dear.identity.member.application.port;

public interface AuthRolePort {

    void promoteToSeller(final Long memberId);

    void demoteToBuyer(final Long memberId);
}
