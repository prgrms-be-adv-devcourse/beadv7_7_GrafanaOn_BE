package shop.dear.commerce.financial.wallet.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import shop.dear.commerce.financial.wallet.domain.model.Wallet;
import shop.dear.commerce.financial.wallet.domain.repository.WalletRepository;
import shop.dear.commerce.financial.wallet.infrastructure.persistence.jpa.WalletJpaRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WalletRepositoryAdapter implements WalletRepository {

    private final WalletJpaRepository walletJpaRepository;

    @Override
    public Optional<Wallet> findById(final Long walletId) {
        return walletJpaRepository.findById(walletId);
    }

    @Override
    public Optional<Wallet> findByMemberId(final Long memberId) {
        return walletJpaRepository.findByMemberId(memberId);
    }

    @Override
    public Wallet save(final Wallet wallet) {
        return walletJpaRepository.save(wallet);
    }

}