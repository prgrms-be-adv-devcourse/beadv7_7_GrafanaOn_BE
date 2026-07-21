package shop.deal.commerce.financial.wallet.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import shop.deal.commerce.financial.wallet.domain.model.Wallet;

public interface WalletJpaRepository extends JpaRepository<Wallet, Long> {
}
