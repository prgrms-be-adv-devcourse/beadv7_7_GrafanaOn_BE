package shop.dear.commerce.financial.wallet.application;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.financial.wallet.application.dto.*;
import shop.dear.commerce.financial.wallet.application.port.WalletLogQueryPort;
import shop.dear.commerce.financial.wallet.domain.costant.WalletLogType;
import shop.dear.commerce.financial.wallet.domain.exception.WalletErrorCode;
import shop.dear.commerce.financial.wallet.domain.model.Wallet;
import shop.dear.commerce.financial.wallet.domain.repository.WalletRepository;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletLogQueryPort walletLogQueryPort;

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GetWalletInfo getWalletId(final Long memberId) {

        final Wallet wallet = findWallet(memberId);

        return GetWalletInfo.from(wallet);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void topUp(final TopUpCommand command) {
        final Wallet wallet = getOrCreateWallet(command.memberId());

        // 원장 처리 전에 중복 여부를 확인
        // 새 Wallet은 아직 ID가 없으므로 조회하지 않는다.
        if (wallet.getId() != null
                && isAlreadyProcessed(
                        wallet,
                        WalletLogType.TOPUP,
                        command.paymentId(),
                        command.amount()
        )) {
            return;
        }

        wallet.topup(command.amount(), command.paymentId());

        walletRepository.save(wallet);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void hold(final HoldCommand command) {
        final Wallet wallet = findWallet(command.memberId());

        // 원장 처리 전에 중복 여부를 확인
        if (isAlreadyProcessed(
                wallet,
                WalletLogType.HOLD,
                command.offerId(),
                command.amount()
        )) {
            return;
        }

        wallet.hold(command.amount(), command.offerId());

        walletRepository.save(wallet);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(final ReleaseCommand command) {
        final Wallet wallet = findWallet(command.memberId());

        // release 요청 건의 heldAmount가 WalletLog에 존재하고 금액이 일치하는 지 확인
        final BigDecimal heldAmount = walletLogQueryPort.findLogAmount(
                wallet.getId(),
                WalletLogType.HOLD,
                command.offerId()
        )
        .orElseThrow(() -> new BusinessException(
                WalletErrorCode.HOLD_NOT_FOUND
        ));

        validateAmountMatches(command.amount(), heldAmount);

        // Release 멱등성 검증
        final Optional<BigDecimal> releasedAmount = walletLogQueryPort.findLogAmount(
                wallet.getId(),
                WalletLogType.RELEASE,
                command.offerId()
        );

        // 멱등성 처리
        if (releasedAmount.isPresent()) {
            validateAmountMatches(command.amount(), releasedAmount.get());
            return;
        }

        try {
            wallet.release(command.amount(), command.offerId());
            walletRepository.save(wallet);
        } catch (final DataIntegrityViolationException exception) {
            throw new BusinessException(WalletErrorCode.DUPLICATE_RELEASE);
        }
    }

    private boolean isAlreadyProcessed(
            final Wallet wallet,
            final WalletLogType type,
            final Long referenceId,
            final BigDecimal requestedAmount
    ) {
        final Optional<BigDecimal> recordedAmount = walletLogQueryPort.findLogAmount(
                wallet.getId(),
                type,
                referenceId
        );

        if (recordedAmount.isEmpty()) {
            return false;
        }

        if (requestedAmount.compareTo(recordedAmount.get()) != 0) {
            throw new BusinessException(
                    WalletErrorCode.WALLET_LOG_AMOUNT_MISMATCH
            );
        }

        return true;
    }

    private void validateAmountMatches(
            final BigDecimal requestedAmount,
            final BigDecimal recordedAmount
    ) {
        if (requestedAmount.compareTo(recordedAmount) != 0) {
            throw new BusinessException(
                    WalletErrorCode.RELEASE_AMOUNT_MISMATCH
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void payAvailable(final PayCommand command) {
        final Wallet wallet = findWallet(command.memberId());

        // 원장 처리 전에 중복 여부를 확인
        if (isAlreadyProcessed(
                wallet,
                WalletLogType.PAYMENT,
                command.paymentId(),
                command.amount()
        )) {
            return;
        }

        wallet.payAvailable(command.amount(), command.paymentId());

        walletRepository.save(wallet);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void payHeld(final PayCommand command) {
        final Wallet wallet = findWallet(command.memberId());

        // 원장 처리 전에 중복 여부를 확인
        if (isAlreadyProcessed(
                wallet,
                WalletLogType.PAYMENT,
                command.paymentId(),
                command.amount()
        )) {
            return;
        }

        wallet.payHeld(command.amount(), command.paymentId());

        walletRepository.save(wallet);
    }

    @Transactional
    public void earn(final EarnCommand command){
        final Wallet wallet  = findById(command.walletId());

        wallet.earn(command.amount(), command.referenceId());

        walletRepository.save(wallet);
    }

    // 정산 지급 - 한 지갑의 정산 건들을 충전한다.
    // 정산 상태 변경과 같은 트랜잭션에서 처리되도록 전파 옵션을 두지 않는다.
    @Transactional
    public void earnAll(final Long walletId, final List<EarnCommand> commands) {
        final Wallet wallet = findById(walletId);

        for (final EarnCommand command : commands) {
            wallet.earn(command.amount(), command.referenceId());
        }

        walletRepository.save(wallet);
    }

    @Transactional
    public void saveWallet(final Long memberId) {
        if (walletRepository.findByMemberId(memberId).isPresent()) {
            return;
        }

        walletRepository.save(Wallet.create(memberId));
    }
}
