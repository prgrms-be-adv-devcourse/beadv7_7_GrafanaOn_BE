package shop.dear.identity.member.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.FetchType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.common.audit.BaseEntity;
import shop.dear.identity.member.domain.constract.SellerStatus;

@Entity
@Table(name = "seller")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seller extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank", nullable = false)
    private String bank;

    @Column(name = "account", nullable = false)
    private String account;

    @Column(name = "status", nullable = false)
    private SellerStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    Seller(
        final String bank,
        final String account,
        final Member member
    ){
        this.bank = bank;
        this.account = account;
        this.member = member;
        this.status = SellerStatus.ACTIVE;
    }

    public void changeAccount(final String bank, final String account) {
        this.bank = bank;
        this.account = account;
    }

    public void withdrawing(){
        this.status = SellerStatus.WITHDRAWING;
    }

    public void withdraw() {
        this.status = SellerStatus.WITHDRAWN;
        this.bank = "****";
        this.account = "****";
    }
}