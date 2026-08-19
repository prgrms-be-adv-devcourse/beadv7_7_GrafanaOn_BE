package shop.dear.commerce.financial.wallet.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.dear.commerce.financial.wallet.domain.costant.WalletLogType;
import shop.dear.commerce.financial.wallet.domain.model.Wallet;

import java.math.BigDecimal;
import java.util.Optional;

public interface WalletJpaRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByMemberId(Long memberId);

    @Query("""
            SELECT log.amount
            FROM Wallet wallet
            JOIN wallet.walletLogs log
            WHERE wallet.id = :walletId
                AND log.type = :type
                AND log.referenceId = :referenceId
            """)
    Optional<BigDecimal> findLogAmount(
            @Param("walletId") Long walletId,
            @Param("type") WalletLogType type,
            @Param("referenceId") Long referenceId
    );
}
