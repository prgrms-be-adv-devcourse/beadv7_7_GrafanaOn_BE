package shop.dear.identity.auth.authentication.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import shop.dear.audit.BaseEntity;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.auth.authentication.domain.exception.AuthErrorCode;
import shop.dear.identity.auth.authorization.domain.Role;

import java.util.Locale;

@Entity
@Table(name = "auth_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", unique = true)
    private Long memberId;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuthAccountStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    private AuthAccount(
            final String email,
            final String passwordHash,
            final AuthProvider provider,
            final String providerId
    ) {
        validateEmail(email);

        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.passwordHash = passwordHash;
        this.provider = provider;
        this.providerId = providerId;
        this.role = Role.BUYER;
        this.status = AuthAccountStatus.PENDING;
    }

    public static AuthAccount create(
            final String email,
            final String passwordHash
    ) {
        validatePasswordHash(passwordHash); // 여기서 패스워드 검증

        return new AuthAccount(email, passwordHash, AuthProvider.LOCAL, null);
    }

    // 소셜로 처음 들어온 사용자의 계정 생성. 비밀번호는 없다.
    public static AuthAccount createSocial(
            final String email,
            final AuthProvider provider,
            final String providerId
    ) {
        return new AuthAccount(email, null, provider, providerId);
    }

    // 이메일로 가입해 둔 기존 계정에 소셜을 연결한다. 비밀번호는 그대로 둔다.
    public void linkSocial(final AuthProvider provider, final String providerId) {
        this.provider = provider;
        this.providerId = providerId;
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    public void activate(final Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID가 누락되었습니다.");
        }

        if (status != AuthAccountStatus.PENDING) {
            throw new IllegalStateException("대기 상태의 계정만 활성화할 수 있습니다.");
        }

        this.memberId = memberId;
        this.status = AuthAccountStatus.ACTIVE; // 활성화, 온전한 계정 생성 완료.
    }

    public void changePasswordHash(final String newPasswordHash) {
        validatePasswordHash(newPasswordHash);
        this.passwordHash = newPasswordHash;
    }

    // 판매자 권한
    public void promoteToSeller() {
        this.role = Role.SELLER;
    }

    // 판매자 권한 해지
    public void demoteToBuyer() {
        this.role = Role.BUYER;
    }

    // 회원 탈퇴
    public void withdraw() {
        if (!isActive()) {
            throw new BusinessException(
                    AuthErrorCode.INACTIVE_ACCOUNT
            );
        }
        this.status = AuthAccountStatus.WITHDRAWN;
    }

    public boolean isActive() {
        return status == AuthAccountStatus.ACTIVE;
    }

    private static void validateEmail(final String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수로 입력해야 합니다.");
        }
    }

    private static void validatePasswordHash(final String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("비밀번호를 입력해 주세요.");
        }
    }
}
