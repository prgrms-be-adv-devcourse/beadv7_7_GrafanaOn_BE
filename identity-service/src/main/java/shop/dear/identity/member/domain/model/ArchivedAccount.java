package shop.dear.identity.member.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.audit.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "archived_account",
    indexes = {
        @Index(name="idx_archived_account",columnList = "expires_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArchivedAccount extends BaseEntity {

    private static final int ACCOUNT_RETENTION_YEARS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "bank", column = @Column(name = "bank", nullable = false)),
        @AttributeOverride(name = "account", column = @Column(name = "account", nullable = false))
    })
    private AccountInfo accountInfo;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

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
        final AccountInfo accountInfo,
        final LocalDateTime withdrawnAt
    ) {

        LocalDateTime expiresAt = withdrawnAt.plusYears(ACCOUNT_RETENTION_YEARS);

        return new ArchivedAccount(memberId, accountInfo, expiresAt);
    }
}
