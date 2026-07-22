package shop.dear.commerce.financial.wallet.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.common.audit.BaseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wallet")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal balance;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.PERSIST)
    private List<WalletLog> logs = new ArrayList<>();

    private Wallet(final Long memberId) {
        this.memberId = memberId;
        this.balance = BigDecimal.ZERO;
    }

    public static Wallet create(final Long memberId) {
        return new Wallet(memberId);
    }

}
