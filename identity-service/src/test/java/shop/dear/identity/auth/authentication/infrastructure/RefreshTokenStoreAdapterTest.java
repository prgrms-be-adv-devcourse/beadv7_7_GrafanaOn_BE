package shop.dear.identity.auth.authentication.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.identity.auth.authentication.application.dto.RefreshTokenVerificationResult;
import shop.dear.identity.auth.authentication.domain.RefreshToken;
import shop.dear.identity.auth.authentication.infrastructure.persistence.RefreshTokenStoreAdapter;
import shop.dear.identity.auth.authentication.infrastructure.persistence.jpa.RefreshTokenJpaRepository;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenStoreAdapterTest {

    private static final String RAW_REFRESH_TOKEN =
            "refresh-token";

    private static final String EXPECTED_TOKEN_HASH =
            "0eb17643d4e9261163783a420859c92c7d212fa9624106a12b510afbec266120";

    @Mock
    private RefreshTokenJpaRepository jpaRepository;

    @InjectMocks
    private RefreshTokenStoreAdapter refreshTokenStoreAdapter;

    @Test
    @DisplayName("Refresh Token은 SHA-256 해시로 저장한다")
    void saveRefreshTokenAsHash() {
        Instant expiresAt =
                Instant.parse("2026-08-01T00:00:00Z");

        given(jpaRepository.findById(1L))
                .willReturn(Optional.empty());

        refreshTokenStoreAdapter.save(
                1L,
                RAW_REFRESH_TOKEN,
                expiresAt
        );

        ArgumentCaptor<RefreshToken> tokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(jpaRepository).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();

        assertEquals(1L, savedToken.getMemberId());
        assertEquals(EXPECTED_TOKEN_HASH, savedToken.getTokenHash());
        assertNotEquals(
                RAW_REFRESH_TOKEN,
                savedToken.getTokenHash()
        );
        assertEquals(expiresAt, savedToken.getExpiresAt());
    }

    @Test
    @DisplayName("현재 저장된 Refresh Token과 일치하면 MATCHED를 반환한다")
    void verifyMatchedRefreshToken() {
        RefreshToken savedToken = RefreshToken.create(
                1L,
                EXPECTED_TOKEN_HASH,
                Instant.parse("2099-08-01T00:00:00Z")
        );

        given(jpaRepository.findById(1L))
                .willReturn(Optional.of(savedToken));

        RefreshTokenVerificationResult result =
                refreshTokenStoreAdapter.verify(
                        1L,
                        RAW_REFRESH_TOKEN
                );

        assertEquals(
                RefreshTokenVerificationResult.MATCHED,
                result
        );

        verify(jpaRepository).findById(1L);
    }

    @Test
    @DisplayName("현재 저장된 Refresh Token과 다르면 MISMATCHED를 반환한다")
    void verifyMismatchedRefreshToken() {
        RefreshToken savedToken = RefreshToken.create(
                1L,
                "different-token-hash",
                Instant.parse("2099-08-01T00:00:00Z")
        );

        given(jpaRepository.findById(1L))
                .willReturn(Optional.of(savedToken));

        RefreshTokenVerificationResult result =
                refreshTokenStoreAdapter.verify(
                        1L,
                        RAW_REFRESH_TOKEN
                );

        assertEquals(
                RefreshTokenVerificationResult.MISMATCHED,
                result
        );
    }

    @Test
    @DisplayName("저장된 Refresh Token이 만료되었으면 EXPIRED를 반환한다")
    void verifyExpiredRefreshToken() {
        RefreshToken savedToken = RefreshToken.create(
                1L,
                EXPECTED_TOKEN_HASH,
                Instant.parse("2020-08-01T00:00:00Z")
        );

        given(jpaRepository.findById(1L))
                .willReturn(Optional.of(savedToken));

        RefreshTokenVerificationResult result =
                refreshTokenStoreAdapter.verify(
                        1L,
                        RAW_REFRESH_TOKEN
                );

        assertEquals(
                RefreshTokenVerificationResult.EXPIRED,
                result
        );
    }

    @Test
    @DisplayName("저장된 Refresh Token이 없으면 NOT_FOUND를 반환한다")
    void verifyNotFoundRefreshToken() {
        given(jpaRepository.findById(1L))
                .willReturn(Optional.empty());

        RefreshTokenVerificationResult result =
                refreshTokenStoreAdapter.verify(
                        1L,
                        RAW_REFRESH_TOKEN
                );

        assertEquals(
                RefreshTokenVerificationResult.NOT_FOUND,
                result
        );
    }

    @Test
    @DisplayName("재사용이 탐지되면 현재 Refresh Token을 삭제한다")
    void revokeCompromisedSession() {
        refreshTokenStoreAdapter.revokeCompromisedSession(1L);

        verify(jpaRepository).deleteById(1L);
    }

    @Test
    @DisplayName("로그아웃하면 회원의 Refresh Token을 삭제한다")
    void deleteRefreshToken() {
        refreshTokenStoreAdapter.deleteByMemberId(1L);

        verify(jpaRepository).deleteById(1L);
    }
}