package shop.dear.identity.auth.authentication.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.auth.authentication.application.dto.*;
import shop.dear.identity.auth.authentication.application.port.MemberProfilePort;
import shop.dear.identity.auth.authentication.application.port.PasswordEncoderPort;
import shop.dear.identity.auth.authentication.application.port.RefreshTokenStorePort;
import shop.dear.identity.auth.authentication.application.port.TokenProviderPort;
import shop.dear.identity.auth.authentication.domain.AuthAccount;
import shop.dear.identity.auth.authentication.domain.AuthAccountRepository;
import shop.dear.identity.auth.authentication.domain.AuthAccountStatus;
import shop.dear.identity.auth.authentication.domain.AuthProvider;
import shop.dear.identity.auth.authentication.domain.exception.AuthErrorCode;
import shop.dear.identity.auth.authorization.domain.Role;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthAccountRepository authAccountRepository;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @Mock
    private MemberProfilePort memberProfilePort;

    @Mock
    private TokenProviderPort tokenProviderPort;

    @Mock
    private RefreshTokenStorePort refreshTokenStorePort;

    @InjectMocks
    private AuthService authService;

    /**
     * 비밀번호가 암호화된 값으로 저장되는가?
     * Member가 반환한 memberId와 연결되는가?
     * 기본 역할이 BUYER인가?
     * 계정이 ACTIVE로 전환되는가?
     */
    @Test
    @DisplayName("회원가입에 성공하면 Auth 계정과 Member 프로필을 생성한다.")
    void signUpSuccess() {
        SignUpCommand command = new SignUpCommand(
                "buyer@example.com",
                "password123",
                "구매자",
                "서울시 강남구",
                "010-1234-5678"
        );

        given(passwordEncoderPort.encode("password123"))
                .willReturn("encoded-password");

        given(memberProfilePort.createProfile(
                "구매자",
                "서울시 강남구",
                "010-1234-5678"
        )).willReturn(
                new MemberProfileResult(1L, "user_000001")
        );

        SignUpResult result = authService.signUp(command);

        ArgumentCaptor<AuthAccount> accountCaptor =
                ArgumentCaptor.forClass(AuthAccount.class);

        verify(authAccountRepository).save(accountCaptor.capture());

        AuthAccount savedAccount = accountCaptor.getValue();

        assertEquals(1L, savedAccount.getMemberId());
        assertEquals("buyer@example.com", savedAccount.getEmail());
        assertEquals("encoded-password", savedAccount.getPasswordHash());
        assertEquals(Role.BUYER, savedAccount.getRole());
        assertTrue(savedAccount.isActive());

        assertEquals(1L, result.memberId());
        assertEquals("buyer@example.com", result.email());
        assertEquals("user_000001", result.nickname());
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 회원가입에 실패한다")
    void signUpFailsWhenEmailAlreadyExists() {
        SignUpCommand command = new SignUpCommand(
                "buyer@example.com",
                "password123",
                "구매자",
                "서울시 강남구",
                "010-1234-5678"
        );

        given(authAccountRepository.existsByEmail(
                "buyer@example.com"
        )).willReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.signUp(command)
        );

        assertSame(
                AuthErrorCode.DUPLICATE_EMAIL,
                exception.getErrorCode()
        );

        verify(passwordEncoderPort, never())
                .encode("password123");

        verify(memberProfilePort, never())
                .createProfile(
                        "구매자",
                        "서울시 강남구",
                        "010-1234-5678"
                );

        verify(authAccountRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("로그인에 성공하면 Access Token과 Refresh Token을 발급한다")
    void loginSuccess() {
        AuthAccount authAccount = createActiveAccount();

        LoginCommand command = new LoginCommand(
                "buyer@example.com",
                "password123"
        );

        TokenResult issuedTokens = new TokenResult(
                "access-token",
                "refresh-token",
                3600L,
                1209600L
        );

        given(authAccountRepository.findByEmail(
                "buyer@example.com"
        )).willReturn(Optional.of(authAccount));

        given(passwordEncoderPort.matches(
                "password123",
                "encoded-password"
        )).willReturn(true);

        given(tokenProviderPort.issueTokens(
                1L,
                Role.BUYER
        )).willReturn(issuedTokens);

        TokenResult result = authService.login(command);

        assertSame(issuedTokens, result);

        verify(tokenProviderPort).issueTokens(
                1L,
                Role.BUYER
        );

        verify(refreshTokenStorePort).save(
                eq(1L),
                eq("refresh-token"),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
    void loginFailsWhenPasswordDoesNotMatch() {
        AuthAccount authAccount = createActiveAccount();

        LoginCommand command = new LoginCommand(
                "buyer@example.com",
                "wrong-password"
        );

        given(authAccountRepository.findByEmail(
                "buyer@example.com"
        )).willReturn(Optional.of(authAccount));

        given(passwordEncoderPort.matches(
                "wrong-password",
                "encoded-password"
        )).willReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(command)
        );

        assertSame(
                AuthErrorCode.INVALID_CREDENTIALS,
                exception.getErrorCode()
        );

        verifyNoInteractions(tokenProviderPort);
        verifyNoInteractions(refreshTokenStorePort);
    }

    private AuthAccount createActiveAccount() {
        AuthAccount authAccount = AuthAccount.create(
                "buyer@example.com",
                "encoded-password"
        );

        authAccount.activate(1L);

        return authAccount;
    }

    @Test
    @DisplayName("유효한 Refresh Token이면 새로운 토큰을 발급하고 저장된 토큰을 교체한다")
    void reissueSuccess() {
        AuthAccount authAccount = createActiveAccount();

        ReissueTokenCommand command =
                new ReissueTokenCommand("old-refresh-token");

        TokenResult reissuedTokens = new TokenResult(
                "new-access-token",
                "new-refresh-token",
                900L,
                1209600L
        );

        given(tokenProviderPort.parseMemberIdFromRefreshToken(
                "old-refresh-token"
        )).willReturn(1L);

        given(refreshTokenStorePort.verify(
                1L,
                "old-refresh-token"
        )).willReturn(RefreshTokenVerificationResult.MATCHED);

        given(authAccountRepository.findByMemberId(1L))
                .willReturn(Optional.of(authAccount));

        given(tokenProviderPort.issueTokens(
                1L,
                Role.BUYER
        )).willReturn(reissuedTokens);

        TokenResult result = authService.reissue(command);

        assertSame(reissuedTokens, result);

        verify(refreshTokenStorePort).save(
                eq(1L),
                eq("new-refresh-token"),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("저장된 Refresh Token과 일치하지 않으면 재발급에 실패한다")
    void reissueFailsWhenRefreshTokenDoesNotMatch() {
        ReissueTokenCommand command =
                new ReissueTokenCommand("invalid-refresh-token");

        given(tokenProviderPort.parseMemberIdFromRefreshToken(
                "invalid-refresh-token"
        )).willReturn(1L);

        given(refreshTokenStorePort.verify(
                1L,
                "invalid-refresh-token"
        )).willReturn(RefreshTokenVerificationResult.NOT_FOUND);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.reissue(command)
        );

        assertSame(
                AuthErrorCode.INVALID_REFRESH_TOKEN,
                exception.getErrorCode()
        );

        verify(authAccountRepository, never())
                .findByMemberId(anyLong());

        verify(tokenProviderPort, never())
                .issueTokens(anyLong(), any(Role.class));
    }

    @Test
    @DisplayName("교체된 Refresh Token이 재사용되면 현재 세션을 폐기한다")
    void reissueRevokesSessionWhenRefreshTokenIsReused() {
        ReissueTokenCommand command =
                new ReissueTokenCommand("reused-refresh-token");

        given(tokenProviderPort.parseMemberIdFromRefreshToken(
                "reused-refresh-token"
        )).willReturn(1L);

        given(refreshTokenStorePort.verify(
                1L,
                "reused-refresh-token"
        )).willReturn(RefreshTokenVerificationResult.MISMATCHED);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.reissue(command)
        );

        assertSame(
                AuthErrorCode.REFRESH_TOKEN_REUSE_DETECTED,
                exception.getErrorCode()
        );

        verify(refreshTokenStorePort)
                .revokeCompromisedSession(1L);

        verify(authAccountRepository, never())
                .findByMemberId(anyLong());

        verify(tokenProviderPort, never())
                .issueTokens(anyLong(), any(Role.class));
    }

    @Test
    @DisplayName("로그아웃하면 회원의 Refresh Token을 삭제한다")
    void logoutSuccess() {
        authService.logout(new LogoutCommand(1L));

        verify(refreshTokenStorePort)
                .deleteByMemberId(1L);
    }

    @Test
    @DisplayName("회원 탈퇴에 성공하면 프로필을 익명화하고 인증 계정과 Refresh Token을 무효화한다")
    void withdrawSuccess() {
        // given
        AuthAccount authAccount = createActiveAccount();

        given(authAccountRepository.findByMemberId(1L))
                .willReturn(Optional.of(authAccount));

        WithdrawCommand command =
                new WithdrawCommand(1L);

        // when
        authService.withdraw(command);

        // then
        assertEquals(
                AuthAccountStatus.WITHDRAWN,
                authAccount.getStatus()
        );

        verify(memberProfilePort)
                .withdrawProfile(1L);

        verify(authAccountRepository)
                .save(authAccount);

        verify(refreshTokenStorePort)
                .deleteByMemberId(1L);
    }

    /**
     * provider + providerId로 이미 연결된 계정을 찾으면
     * 새로 만들지 않고 그 계정으로 토큰만 발급하는가?
     */
    @Test
    @DisplayName("이미 연결된 소셜 계정이면 조회만 하고 토큰을 발급한다")
    void loginWithSocialUsesLinkedAccount() {
        AuthAccount linked = AuthAccount.createSocial(
                "user@gmail.com",
                AuthProvider.GOOGLE,
                "google-123"
        );

        linked.activate(1L);

        given(authAccountRepository.findByProviderAndProviderId(
                AuthProvider.GOOGLE,
                "google-123"
        )).willReturn(Optional.of(linked));

        given(tokenProviderPort.issueTokens(
                1L,
                Role.BUYER
        )).willReturn(socialTokens());

        TokenResult result = authService.loginWithSocial(googleCommand(true));

        assertSame(socialTokens().accessToken(), result.accessToken());

        // 이메일 조회도, 프로필 생성도 일어나면 안 된다
        verify(authAccountRepository, never()).findByEmail(any());
        verify(memberProfilePort, never()).createProfile(any(), any(), any());
    }

    /**
     * 같은 이메일의 기존 계정이 있으면 새로 만들지 않고 연동하는가?
     * 연동 후에도 비밀번호가 남아 두 방식 모두 로그인할 수 있는가?
     */
    @Test
    @DisplayName("같은 이메일의 기존 계정이 있으면 소셜을 연동한다")
    void loginWithSocialLinksExistingAccount() {
        AuthAccount existing = AuthAccount.create(
                "user@gmail.com",
                "encoded-password"
        );

        existing.activate(1L);

        given(authAccountRepository.findByProviderAndProviderId(
                any(),
                any()
        )).willReturn(Optional.empty());

        given(authAccountRepository.findByEmail(
                "user@gmail.com"
        )).willReturn(Optional.of(existing));

        given(authAccountRepository.save(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        given(tokenProviderPort.issueTokens(
                1L,
                Role.BUYER
        )).willReturn(socialTokens());

        authService.loginWithSocial(googleCommand(true));

        assertEquals(AuthProvider.GOOGLE, existing.getProvider());
        assertEquals("google-123", existing.getProviderId());

        // 비밀번호를 지우면 기존 사용자가 비밀번호로 로그인할 수 없게 된다
        assertTrue(existing.hasPassword());

        verify(memberProfilePort, never()).createProfile(any(), any(), any());
    }

    /**
     * 확인되지 않은 이메일로 연동을 허용하면
     * 남의 이메일로 소셜 계정을 만들어 기존 계정을 가져갈 수 있다.
     */
    @Test
    @DisplayName("이메일이 확인되지 않았으면 기존 계정에 연동하지 않는다")
    void loginWithSocialRejectsUnverifiedEmail() {
        AuthAccount existing = AuthAccount.create(
                "user@gmail.com",
                "encoded-password"
        );

        existing.activate(1L);

        given(authAccountRepository.findByProviderAndProviderId(
                any(),
                any()
        )).willReturn(Optional.empty());

        given(authAccountRepository.findByEmail(
                "user@gmail.com"
        )).willReturn(Optional.of(existing));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.loginWithSocial(googleCommand(false))
        );

        assertEquals(AuthErrorCode.UNVERIFIED_SOCIAL_EMAIL, exception.getErrorCode());
        assertNull(existing.getProviderId());

        verify(authAccountRepository, never()).save(any());
    }

    /**
     * 연결된 계정도 같은 이메일 계정도 없으면
     * 새 계정과 Member 프로필을 함께 만드는가?
     */
    @Test
    @DisplayName("연결된 계정도 같은 이메일 계정도 없으면 새로 가입시킨다")
    void loginWithSocialCreatesNewAccount() {
        given(authAccountRepository.findByProviderAndProviderId(
                any(),
                any()
        )).willReturn(Optional.empty());

        given(authAccountRepository.findByEmail(
                "user@gmail.com"
        )).willReturn(Optional.empty());

        given(memberProfilePort.createProfile(
                any(),
                any(),
                any()
        )).willReturn(new MemberProfileResult(1L, "user_000001"));

        given(authAccountRepository.save(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        given(tokenProviderPort.issueTokens(
                1L,
                Role.BUYER
        )).willReturn(socialTokens());

        authService.loginWithSocial(googleCommand(true));

        ArgumentCaptor<AuthAccount> captor = ArgumentCaptor.forClass(AuthAccount.class);
        verify(authAccountRepository).save(captor.capture());

        AuthAccount saved = captor.getValue();

        assertEquals(AuthProvider.GOOGLE, saved.getProvider());
        assertEquals("google-123", saved.getProviderId());
        assertEquals(AuthAccountStatus.ACTIVE, saved.getStatus());

        // 소셜 전용 계정은 비밀번호가 없어야 한다
        assertFalse(saved.hasPassword());
    }

    private SocialLoginCommand googleCommand(final boolean emailVerified) {
        return new SocialLoginCommand(
                AuthProvider.GOOGLE,
                "google-123",
                "user@gmail.com",
                emailVerified,
                "사용자"
        );
    }

    private TokenResult socialTokens() {
        return new TokenResult(
                "access-token",
                "refresh-token",
                3600L,
                1209600L
        );
    }
}