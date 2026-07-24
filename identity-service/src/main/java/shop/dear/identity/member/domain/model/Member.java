package shop.dear.identity.member.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.common.audit.BaseEntity;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.member.domain.constract.SellerStatus;
import shop.dear.identity.member.domain.exception.MemberErrorCode;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 30, nullable = false)
    private String name;

    @Column(name = "default_shipping_address",nullable = false)
    private String defaultShippingAddress;

    @Column(name = "phone_number", length = 20, nullable = false)
    private String phoneNumber;

    @Column(name = "nickname", nullable = false, unique = true)
    private String nickname;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Seller seller;

    private Member(
        final String name,
        final String defaultShippingAddress,
        final String phoneNumber,
        final String nickname
    ) {
        this.name = name;
        this.defaultShippingAddress = defaultShippingAddress;
        this.phoneNumber = phoneNumber;
        this.nickname = nickname;
    }

    public static Member create(
        final String name,
        final String defaultShippingAddress,
        final String phoneNumber,
        final String nickname
    ) {
        return new Member(
            name,
            defaultShippingAddress,
            phoneNumber,
            nickname
        );
    }

    public void changeProfile(
        final String defaultShippingAddress,
        final String phoneNumber,
        final String nickname
    ) {
        this.defaultShippingAddress = defaultShippingAddress;
        this.phoneNumber = phoneNumber;
        this.nickname = nickname;
    }

    public boolean isSeller(){
        return this.seller != null && this.seller.getStatus() == SellerStatus.ACTIVE;
    }

    public void registerSeller(final String bank, final String account) {
        if (this.isSeller()) {
            throw new BusinessException(MemberErrorCode.ALREADY_SELLER);
        }
        this.seller = new Seller(
            bank,
            account,
            this
        );
    }

    public void updateSellerAccount(String bank, String account){
        if (!this.isSeller()) {
            throw new BusinessException(MemberErrorCode.NOT_SELLER);
        }
        this.seller.changeAccount(bank,account);
    }

    public void requestSellerWithdrawal() {
        if (!this.isSeller()) {
            throw new BusinessException(MemberErrorCode.NOT_SELLER);
        }
        this.seller.withdrawing();
    }

    public void completeSellerWithdrawal(){
        this.seller.withdraw();
    }
}
