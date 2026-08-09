package shop.dear.identity.auth.authentication.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.dear.identity.auth.authentication.domain.RefreshToken;

import java.time.Instant;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long> {
}
