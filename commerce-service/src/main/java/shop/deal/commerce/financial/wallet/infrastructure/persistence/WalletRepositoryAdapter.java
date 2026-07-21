package shop.deal.commerce.financial.wallet.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.deal.commerce.financial.wallet.domain.repository.WalletRepository;
import shop.deal.commerce.financial.wallet.infrastructure.persistence.jpa.WalletJpaRepository;

@Repository
@RequiredArgsConstructor
public class WalletRepositoryAdapter implements WalletRepository {

    private final WalletJpaRepository walletJpaRepository;

}