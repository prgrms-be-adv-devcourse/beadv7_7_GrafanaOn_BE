package shop.dear.identity.member.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.audit.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "archived_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArchivedAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Embedded
    private AccountInfo accountInfo;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    //계좌정보 법적 보유기간
    private static final int ACCOUNT_RETENTION_YEARS = 5;

    private ArchivedAccount(
        final Long memberId,
        final AccountInfo accountInfo,
        final LocalDateTime expiresAt
    ) {
        this.memberId = memberId;
        this.accountInfo = accountInfo;
        this.expiresAt = expiresAt;
    }

    public static ArchivedAccount create(
        final Long memberId,
        final AccountInfo accountInfo
    ) {

        LocalDateTime expiresAt = LocalDateTime.now().plusYears(ACCOUNT_RETENTION_YEARS);

        return new ArchivedAccount(memberId, accountInfo, expiresAt);
    }

    public static ArchivedAccount from(final Seller seller) {

        return create(seller.getMember().getId(), seller.getAccountInfo());
    }
}
