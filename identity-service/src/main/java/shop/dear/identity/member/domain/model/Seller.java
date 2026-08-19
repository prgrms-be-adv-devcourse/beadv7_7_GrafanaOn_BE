package shop.dear.identity.member.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.FetchType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.audit.BaseEntity;
import shop.dear.identity.member.domain.constract.SellerStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seller extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //보관 이관(archive) 시 계좌정보를 비우므로 NOT NULL 제약을 두지 않는다
    @Embedded
    private AccountInfo accountInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SellerStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    private Seller(
        final AccountInfo accountInfo,
        final Member member
    ){
        this.accountInfo = accountInfo;
        this.member = member;
        this.registeredAt = LocalDateTime.now();
        this.status = SellerStatus.ACTIVE;
    }

    static Seller create(final AccountInfo accountInfo, final Member member) {
        Seller seller = new Seller(accountInfo, member);
        return seller;
    }

    void activate(final AccountInfo accountInfo) {
        this.accountInfo = accountInfo;
        this.registeredAt = LocalDateTime.now();
        this.withdrawnAt = null;
        this.status = SellerStatus.ACTIVE;
    }

    void updateAccount(final AccountInfo accountInfo) {

        if (!this.accountInfo.equals(accountInfo)) {
            this.accountInfo = accountInfo;
        }
    }

    void withdraw() {
        this.withdrawnAt = LocalDateTime.now();
        this.status = SellerStatus.WITHDRAWN;
    }

    public Long getMemberId() {
        return this.member.getId();
    }

    public void archive() {
        this.status = SellerStatus.ARCHIVED;
        this.accountInfo = null;
    }
}
