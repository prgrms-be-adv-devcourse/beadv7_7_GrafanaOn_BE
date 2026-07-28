package shop.dear.commerce.financial.wallet.application;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shop.dear.commerce.financial.wallet.application.dto.*;
import shop.dear.commerce.financial.wallet.domain.exception.WalletErrorCode;
import shop.dear.commerce.financial.wallet.domain.model.Wallet;
import shop.dear.commerce.financial.wallet.domain.repository.WalletRepository;
import shop.dear.common.exception.BusinessException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private final WalletRepository walletRepository;

    private Wallet findWallet(final Long memberId) {
        return walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(WalletErrorCode.WALLET_NOT_FOUND));
    }

    private Wallet findById(final Long walletId) {
        return walletRepository.findById(walletId)
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

    public GetWalletInfo getWalletId(final Long memberId) {

        final Wallet wallet = findWallet(memberId);

        return GetWalletInfo.from(wallet);
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

    @Transactional
    public void payAvailable(final PayCommand command) {
        final Wallet wallet = findWallet(command.memberId());

        wallet.payAvailable(command.amount(), command.paymentId());

        walletRepository.save(wallet);
    }

    @Transactional
    public void payHeld(final PayCommand command) {
        final Wallet wallet = findWallet(command.memberId());

        wallet.payHeld(command.amount(), command.paymentId());

        walletRepository.save(wallet);
    }

    @Transactional
    public void earn(final EarnCommand command){
        final Wallet wallet  = findById(command.walletId());

        wallet.earn(command.amount(), command.settlementId());

        walletRepository.save(wallet);
    }

    // 정산 지급 - 한 지갑의 정산 건들을 충전한다.
    // 정산 상태 변경과 같은 트랜잭션에서 처리되도록 전파 옵션을 두지 않는다.
    @Transactional
    public void earnAll(final Long walletId, final List<EarnCommand> commands) {
        final Wallet wallet = findById(walletId);

        for (final EarnCommand command : commands) {
            wallet.earn(command.amount(), command.settlementId());
        }

        walletRepository.save(wallet);
    }
}
