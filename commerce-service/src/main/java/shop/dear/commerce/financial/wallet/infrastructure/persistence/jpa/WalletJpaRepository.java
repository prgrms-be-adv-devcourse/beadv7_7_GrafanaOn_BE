package shop.dear.commerce.financial.wallet.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import shop.dear.commerce.financial.wallet.domain.model.Wallet;

public interface WalletJpaRepository extends JpaRepository<Wallet, Long> {
}
