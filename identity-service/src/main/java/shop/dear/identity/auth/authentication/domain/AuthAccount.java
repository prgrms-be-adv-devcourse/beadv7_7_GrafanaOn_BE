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

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuthAccountStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    private AuthAccount(
            final String email,
            final String passwordHash
    ) {
        validateEmail(email);
        validatePasswordHash(passwordHash);

        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.passwordHash = passwordHash;
        this.role = Role.BUYER;
        this.status = AuthAccountStatus.PENDING;
    }

    public static AuthAccount create(
            final String email,
            final String passwordHash
    ) {
        return new AuthAccount(email, passwordHash);
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

    private void validateEmail(final String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수로 입력해야 합니다.");
        }
    }

    private void validatePasswordHash(final String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("비밀번호를 입력해 주세요.");
        }
    }



}
