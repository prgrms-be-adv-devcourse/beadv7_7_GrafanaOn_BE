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
import shop.dear.identity.auth.authentication.domain.AuthAccount;
import shop.dear.identity.auth.authentication.domain.AuthAccountRepository;
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
                3600L,
                1209600L
        );

        given(tokenProviderPort.parseMemberIdFromRefreshToken(
                "old-refresh-token"
        )).willReturn(1L);

        given(refreshTokenStorePort.matches(
                1L,
                "old-refresh-token"
        )).willReturn(true);

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

        given(refreshTokenStorePort.matches(
                1L,
                "invalid-refresh-token"
        )).willReturn(false);

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
    @DisplayName("로그아웃하면 회원의 Refresh Token을 삭제한다")
    void logoutSuccess() {
        authService.logout(new LogoutCommand(1L));

        verify(refreshTokenStorePort)
                .deleteByMemberId(1L);
    }


}