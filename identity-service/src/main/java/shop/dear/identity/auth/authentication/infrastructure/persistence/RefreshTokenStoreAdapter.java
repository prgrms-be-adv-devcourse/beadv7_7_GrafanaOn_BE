package shop.dear.identity.auth.authentication.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.identity.auth.authentication.application.RefreshTokenStorePort;
import shop.dear.identity.auth.authentication.domain.RefreshToken;
import shop.dear.identity.auth.authentication.infrastructure.persistence.jpa.RefreshTokenJpaRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

// AuthService -> RefreshTokenStorePort <- RefreshTokenStoreAdapter
@Repository
@RequiredArgsConstructor
public class RefreshTokenStoreAdapter implements RefreshTokenStorePort {
    private final RefreshTokenJpaRepository jpaRepository;

    @Override
    public void save(Long memberId, String refreshToken, Instant expiresAt) {
        // JWT 원문을 SHA-256 해시 변환
        String tokenHash = hash(refreshToken);

        // 기존 토큰 존재 시 새로운 해시와 만료 시각으로 교체, 없으면 생성
        RefreshToken entity = jpaRepository.findById(memberId)
                .map(savedToken -> {
                    savedToken.rotate(tokenHash, expiresAt);
                    return savedToken;
                })
                .orElseGet(() ->
                        RefreshToken.create(memberId, tokenHash, expiresAt)
                );

        jpaRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean matches(Long memberId, String refreshToken) {
        String tokenHash = hash(refreshToken);

        return jpaRepository
                .existsByMemberIdAndTokenHashAndExpiresAtAfter(
                        memberId,
                        tokenHash,
                        Instant.now());
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        jpaRepository.deleteById(memberId);
    }

    private String hash(String refreshToken) {
        try {
            MessageDigest messageDigest =
                    MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(
                    refreshToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("암호화 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
