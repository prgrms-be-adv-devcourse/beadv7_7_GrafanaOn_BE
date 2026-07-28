package shop.dear.commerce.financial.wallet.domain.repository;

import shop.dear.commerce.financial.wallet.domain.model.Wallet;
import java.util.Optional;

public interface WalletRepository {
    Optional<Wallet> findByMemberId(Long memberId);

    Wallet save(Wallet wallet);
}
