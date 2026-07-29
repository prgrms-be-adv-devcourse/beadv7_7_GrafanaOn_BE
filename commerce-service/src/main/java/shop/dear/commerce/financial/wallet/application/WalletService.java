package shop.dear.commerce.financial.wallet.application;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shop.dear.commerce.financial.wallet.application.dto.WalletInfo;
import shop.dear.commerce.financial.wallet.application.dto.GetWalletQuery;
import shop.dear.commerce.financial.wallet.application.dto.TopUpCommand;
import shop.dear.commerce.financial.wallet.application.dto.HoldCommand;
import shop.dear.commerce.financial.wallet.application.dto.ReleaseCommand;
import shop.dear.commerce.financial.wallet.application.dto.PayCommand;
import shop.dear.commerce.financial.wallet.domain.exception.WalletErrorCode;
import shop.dear.commerce.financial.wallet.domain.model.Wallet;
import shop.dear.commerce.financial.wallet.domain.repository.WalletRepository;
import shop.dear.common.exception.BusinessException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private final WalletRepository walletRepository;

    private Wallet findWallet(final Long memberId) {
        return walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(WalletErrorCode.WALLET_NOT_FOUND));
    }

    private Wallet getOrCreateWallet(final Long memberId) {
        return walletRepository.findByMemberId(memberId)
                .orElseGet(() -> Wallet.create(memberId));
    }

    public WalletInfo getWallet(final GetWalletQuery query) {
        final Wallet wallet = findWallet(query.memberId());

        return WalletInfo.from(wallet);
    }

    @Transactional
    public void topUp(final TopUpCommand command) {
        final Wallet wallet = getOrCreateWallet(command.memberId());

        wallet.topup(command.amount(), command.paymentId());

        walletRepository.save(wallet);
    }

    @Transactional
    public void hold(final HoldCommand command) {
        final Wallet wallet = findWallet(command.memberId());

        wallet.hold(command.amount(), command.offerId());

        walletRepository.save(wallet);
    }

    @Transactional
    public void release(final ReleaseCommand command) {
        final Wallet wallet = findWallet(command.memberId());

        wallet.release(command.amount(), command.offerId());

        walletRepository.save(wallet);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void payAvailable(final PayCommand command) {
        final Wallet wallet = findWallet(command.memberId());

        wallet.payAvailable(command.amount(), command.paymentId());

        walletRepository.save(wallet);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void payHeld(final PayCommand command) {
        final Wallet wallet = findWallet(command.memberId());

        wallet.payHeld(command.amount(), command.paymentId());

        walletRepository.save(wallet);
    }
}
