package shop.dear.commerce.financial.wallet.application.port;

import shop.dear.commerce.financial.wallet.domain.costant.WalletLogType;
import java.math.BigDecimal;
import java.util.Optional;

public interface ReleaseValidationQueryPort {

    Optional<BigDecimal> findLogAmount(
            Long walletId,
            WalletLogType type,
            Long referenceId
    );
}
