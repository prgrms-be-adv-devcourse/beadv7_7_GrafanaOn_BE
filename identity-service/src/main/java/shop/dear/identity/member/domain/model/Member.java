package shop.dear.identity.member.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.deal.common.audit.BaseEntity;
import shop.deal.identity.member.domain.constract.MemberStatus;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MemberStatus status;

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
        this.status = MemberStatus.BUYER;
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

    public void registerAsSeller(final String bank, final String account) {
        if (this.status == MemberStatus.SELLER) {
            throw new IllegalStateException("이미 판매자로 등록된 회원입니다.");
        }

        this.status = MemberStatus.SELLER;
        this.seller = new Seller(
            bank,
            account,
            this
        );
    }
}
