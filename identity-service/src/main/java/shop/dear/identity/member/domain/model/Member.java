package shop.dear.identity.member.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.audit.BaseEntity;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.member.domain.constract.MemberStatus;
import shop.dear.identity.member.domain.constract.SellerStatus;
import shop.dear.identity.member.domain.exception.MemberErrorCode;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "member",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_nickname", columnNames = "nickname")
    }
)
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

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Seller seller;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MemberStatus status;

    @Column(name = "withdrawn_at", nullable = true)
    private LocalDateTime withdrawnAt;

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
        this.status = MemberStatus.ACTIVE;
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

    public void updateProfile(
        final String defaultShippingAddress,
        final String phoneNumber,
        final String nickname
    ) {
        this.defaultShippingAddress = defaultShippingAddress;
        this.phoneNumber = phoneNumber;
        this.nickname = nickname;
    }

    public void anonymizeProfile() {
        if (seller != null && seller.getStatus() == SellerStatus.ACTIVE) {
            throw new BusinessException(
                MemberErrorCode.SELLER_WITHDRAWAL_REQUIRED
            );
        }

        this.name = "탈퇴한 회원";
        this.defaultShippingAddress = "";
        this.phoneNumber = "";
        this.nickname = "withdrawn_" + this.id;
        this.status = MemberStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
    }

    public boolean isActive(){
        return this.status == MemberStatus.ACTIVE;
    }

    public boolean isSellerActive(){
        return this.seller != null && this.seller.getStatus() == SellerStatus.ACTIVE;
    }

    public boolean isSellerWithdrawn(){
        return this.seller != null && this.seller.getStatus() != SellerStatus.ACTIVE;
    }

    public void registerSeller(final AccountInfo accountInfo) {
        if (this.isSellerActive()) {
            throw new BusinessException(MemberErrorCode.ALREADY_SELLER);
        }

        if (this.isSellerWithdrawn()) {
            this.seller.activate(accountInfo);
            return;
        }

        if (this.seller != null) {
            throw new BusinessException(MemberErrorCode.ALREADY_SELLER);
        }

        this.seller = Seller.create(
            accountInfo,
            this
        );
    }

    public AccountInfo getSellerAccountInfo() {
        if (!this.isSellerActive()) {
            throw new BusinessException(MemberErrorCode.NOT_SELLER);
        }

        return this.seller.getAccountInfo();
    }

    public void updateSellerAccount(final AccountInfo accountInfo){
        if (!this.isSellerActive()) {
            throw new BusinessException(MemberErrorCode.NOT_SELLER);
        }

        this.seller.updateAccount(accountInfo);
    }

    public void withdrawSeller() {
        if (!this.isSellerActive()) {
            throw new BusinessException(MemberErrorCode.NOT_SELLER);
        }
        this.seller.withdraw();
    }

    /**
     * 탈퇴한 판매자의 계좌정보를 보관 대상으로 넘기고 셀러에서 제거한다.
     * 계좌정보를 지우기 전에 스냅샷을 먼저 만들어 반환하므로 호출 순서에 의존하지 않는다.
     */
    public SellerAccountSnapshot archiveSeller() {

        if (this.seller == null || this.seller.getStatus() != SellerStatus.WITHDRAWN) {
            throw new BusinessException(MemberErrorCode.SELLER_NOT_WITHDRAWN);
        }

        SellerAccountSnapshot snapshot = new SellerAccountSnapshot(
            this.id,
            this.seller.getAccountInfo(),
            this.seller.getWithdrawnAt()
        );

        this.seller.archive();

        return snapshot;
    }
}