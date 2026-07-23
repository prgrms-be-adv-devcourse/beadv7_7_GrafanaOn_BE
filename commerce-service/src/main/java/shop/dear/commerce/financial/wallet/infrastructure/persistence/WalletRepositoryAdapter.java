package shop.dear.commerce.financial.wallet.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.financial.wallet.domain.repository.WalletRepository;
import shop.dear.commerce.financial.wallet.infrastructure.persistence.jpa.WalletJpaRepository;

@Repository
@RequiredArgsConstructor
public class WalletRepositoryAdapter implements WalletRepository {

    private final WalletJpaRepository walletJpaRepository;

}