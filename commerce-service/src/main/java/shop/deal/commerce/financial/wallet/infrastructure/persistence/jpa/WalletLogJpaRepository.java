package shop.deal.commerce.financial.wallet.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import shop.deal.commerce.financial.wallet.domain.model.WalletLog;

public interface WalletLogJpaRepository extends JpaRepository<WalletLog, Long> {
}
