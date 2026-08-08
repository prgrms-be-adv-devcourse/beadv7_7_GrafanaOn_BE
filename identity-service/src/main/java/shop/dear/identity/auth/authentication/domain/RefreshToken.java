package shop.dear.identity.auth.authentication.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.audit.BaseEntity;

import java.time.Instant;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    private RefreshToken(Long memberId, String tokenHash, Instant expiresAt) {
        validate(memberId, tokenHash, expiresAt);

        this.memberId = memberId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken create(
            Long memberId,
            String tokenHash,
            Instant expiresAt
    ) {
        return new RefreshToken(memberId, tokenHash, expiresAt);
    }

    // 재로그인이나 재발급 시 행을 다시 추가하는 것이 아니라 rotate로 교체
    public void rotate(String tokenHash, Instant expiresAt) {
        validate(memberId, tokenHash, expiresAt);
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    private void validate(Long memberId, String tokenHash, Instant expiresAt) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID가 누락되었습니다.");
        }

        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("Refresh Token 해시가 누락되었습니다.");
        }

        if (expiresAt == null) {
            throw new IllegalArgumentException("Refresh Token 만료 시각이 누락되었습니다.");
        }
    }
}
