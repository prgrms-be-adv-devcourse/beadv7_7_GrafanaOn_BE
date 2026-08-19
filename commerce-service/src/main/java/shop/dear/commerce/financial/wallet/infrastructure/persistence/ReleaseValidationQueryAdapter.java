package shop.dear.commerce.financial.wallet.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import shop.dear.commerce.financial.wallet.application.port.ReleaseValidationQueryPort;
import shop.dear.commerce.financial.wallet.domain.costant.WalletLogType;
import shop.dear.commerce.financial.wallet.infrastructure.persistence.jpa.WalletJpaRepository;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReleaseValidationQueryAdapter implements ReleaseValidationQueryPort {

    private final WalletJpaRepository walletJpaRepository;

    @Override
    public Optional<BigDecimal> findLogAmount(
            final Long walletId,
            final WalletLogType type,
            final Long referenceId
    ) {
        return walletJpaRepository.findLogAmount(
                walletId,
                type,
                referenceId
        );
    }
}
