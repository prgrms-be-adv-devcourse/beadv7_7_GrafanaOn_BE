package shop.dear.identity.auth.authentication.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.auth.authentication.application.dto.*;
import shop.dear.identity.auth.authentication.application.port.MemberProfilePort;
import shop.dear.identity.auth.authentication.application.port.PasswordEncoderPort;
import shop.dear.identity.auth.authentication.application.port.RefreshTokenStorePort;
import shop.dear.identity.auth.authentication.application.port.TokenProviderPort;
import shop.dear.identity.auth.authentication.domain.AuthAccount;
import shop.dear.identity.auth.authentication.domain.AuthAccountRepository;
import shop.dear.identity.auth.authentication.domain.exception.AuthErrorCode;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final AuthAccountRepository authAccountRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final MemberProfilePort memberProfilePort;

    private final TokenProviderPort tokenProviderPort;
    private final RefreshTokenStorePort refreshTokenStorePort;

    /**
     * [회원가입 순서]
     * 1. 이메일 중복 확인
     * 2. 비밀번호 BCrypt 암호화
     * 3. Member 프로필 생성
     * 4. memberId 연결 및 ACTIVE 전환
     * 5. AuthAccount 저장
     */
    @Transactional
    public SignUpResult signUp(SignUpCommand command) {
        if (authAccountRepository.existsByEmail(command.email())) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        String passwordHash = passwordEncoderPort.encode(command.rawPassword());
        AuthAccount authAccount = AuthAccount.create(command.email(), passwordHash);

        MemberProfileResult memberProfile =
                memberProfilePort.createProfile(
                        command.name(),
                        command.defaultShippingAddress(),
                        command.phoneNumber()
                );

        authAccount.activate(memberProfile.memberId());
        authAccountRepository.save(authAccount);

        return new SignUpResult(
                memberProfile.memberId(),
                authAccount.getEmail(),
                memberProfile.nickname()
        );
    }

    /**
     * [로그인 순서]
     * 1. 이메일로 AuthAccount 조회
     * 2. ACTIVE 계정인지 확인
     * 3. Access/Refrsh Token 발급
     * 4. Refresh Token 해시 저장
     * 5. TokenResult 반환
     */
    @Transactional
    public TokenResult login(LoginCommand command) {
        AuthAccount authAccount = authAccountRepository
                .findByEmail(command.email())
                .orElseThrow(() -> new BusinessException(
                        AuthErrorCode.INVALID_CREDENTIALS
                ));

        boolean passwordMatches = passwordEncoderPort.matches(
                command.rawPassword(),
                authAccount.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new BusinessException(
                    AuthErrorCode.INVALID_CREDENTIALS
            );
        }

        return issueAndStoreTokens(authAccount);
    }

    /**
     * [재발급 흐름]
     * 1. JWT에서 memberId 추출
     * 2. DB에 저장된 Refresh Token 해시와 비교
     * 3. AuthAccount 및 ACTIVE 상태 확인
     * 4. 현재 Role로 새 토큰 발급
     * 5. 기존 Refresh Token을 새 토큰으로 교체
     */
    @Transactional
    public TokenResult reissue(ReissueTokenCommand command) {
        String refreshToken = command.refreshToken();

        Long memberId = tokenProviderPort
                .parseMemberIdFromRefreshToken(refreshToken);

        RefreshTokenVerificationResult verificationResult =
                refreshTokenStorePort.verify(memberId, refreshToken);

        switch (verificationResult) {
            case MISMATCHED -> {
                refreshTokenStorePort.revokeCompromisedSession(memberId);

                throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
            }

            case NOT_FOUND -> throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);

            case EXPIRED -> throw new BusinessException(AuthErrorCode.EXPIRED_TOKEN);

            case MATCHED -> {
                // 정상. 하던 것 계속.
            }
        }

        AuthAccount authAccount = authAccountRepository
                .findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(
                        AuthErrorCode.AUTH_ACCOUNT_NOT_FOUND
                ));
        return issueAndStoreTokens(authAccount);
    }

    @Transactional
    public void logout(LogoutCommand command) {
        refreshTokenStorePort.deleteByMemberId(
                command.memberId()
        );
    }

    /**
     * [회원 탈퇴 흐름]
     * 1. memberId로 AuthAccount 조회
     * 2. Member 프로필 개인정보 익명화 (공백 처리)
     * 2.1 정산 완료가 되지 않은 판매자는 MB-007 예외
     * 3. AuthAccount를 WITHDRAWN 상태로 변경
     * 4. Refresh Token 폐기
     */
    @Transactional
    public void withdraw(final WithdrawCommand command) {
        AuthAccount authAccount = authAccountRepository
                .findByMemberId(command.memberId())
                .orElseThrow(() -> new BusinessException(
                        AuthErrorCode.AUTH_ACCOUNT_NOT_FOUND
                ));

        memberProfilePort.withdrawProfile(command.memberId());

        authAccount.withdraw();
        authAccountRepository.save(authAccount);

        refreshTokenStorePort.deleteByMemberId(command.memberId());
    }

    private TokenResult issueAndStoreTokens(AuthAccount authAccount) {
        if (!authAccount.isActive()) {
            throw new BusinessException(
                    AuthErrorCode.INACTIVE_ACCOUNT
            );
        }

        TokenResult tokenResult = tokenProviderPort.issueTokens(
                authAccount.getMemberId(),
                authAccount.getRole()
        );

        Instant refreshTokenExpiresAt = Instant.now()
                .plusSeconds(
                        tokenResult.refreshTokenExpiresInSeconds()
                );

        refreshTokenStorePort.save(
                authAccount.getMemberId(),
                tokenResult.refreshToken(),
                refreshTokenExpiresAt
        );

        return tokenResult;
    }
}
