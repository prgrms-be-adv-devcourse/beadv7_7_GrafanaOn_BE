package shop.dear.identity.auth.authentication.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.identity.auth.authentication.domain.RefreshToken;
import shop.dear.identity.auth.authentication.infrastructure.persistence.RefreshTokenStoreAdapter;
import shop.dear.identity.auth.authentication.infrastructure.persistence.jpa.RefreshTokenJpaRepository;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("전달받은 Refresh Token을 해시로 변환하여 저장된 값과 비교한다")
    void matchesRefreshTokenByHash() {
        given(
                jpaRepository
                        .existsByMemberIdAndTokenHashAndExpiresAtAfter(
                                eq(1L),
                                eq(EXPECTED_TOKEN_HASH),
                                any(Instant.class)
                        )
        ).willReturn(true);

        boolean matches = refreshTokenStoreAdapter.matches(
                1L,
                RAW_REFRESH_TOKEN
        );

        assertTrue(matches);

        verify(jpaRepository)
                .existsByMemberIdAndTokenHashAndExpiresAtAfter(
                        eq(1L),
                        eq(EXPECTED_TOKEN_HASH),
                        any(Instant.class)
                );
    }

    @Test
    @DisplayName("로그아웃하면 회원의 Refresh Token을 삭제한다")
    void deleteRefreshToken() {
        refreshTokenStoreAdapter.deleteByMemberId(1L);

        verify(jpaRepository).deleteById(1L);
    }
}