package shop.dear.commerce.financial.wallet.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import shop.dear.commerce.financial.wallet.application.dto.*;
import shop.dear.commerce.financial.wallet.application.port.WalletLogQueryPort;
import shop.dear.commerce.financial.wallet.domain.costant.WalletLogType;
import shop.dear.commerce.financial.wallet.domain.exception.WalletErrorCode;
import shop.dear.commerce.financial.wallet.domain.model.Wallet;
import shop.dear.commerce.financial.wallet.domain.repository.WalletRepository;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WalletServiceTest {
    private FakeWalletRepository walletRepository;
    private WalletService walletService;
    private FakeWalletLogQueryPort walletLogQueryPort;

    @BeforeEach
    void setUp() {
        walletRepository = new FakeWalletRepository();
        walletLogQueryPort = new FakeWalletLogQueryPort();

        walletService = new WalletService(
                walletRepository,
                walletLogQueryPort);
    }

    @Test
    @DisplayName("존재하지 않는 지갑을 조회하면 WALLET_NOT_FOUND 예외가 발생한다.")
    void getWallet_whenWalletNotFound_throwsException() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> walletService.getWallet(new GetWalletQuery(1L))
        );

        assertEquals(WalletErrorCode.WALLET_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("지갑이 없을 때 충전하면 새 지갑을 생성하고 저장한다.")
    void topUp_whenWalletNotExists_createsAndSavesWallet() {
        walletService.topUp(
                new TopUpCommand(1L, BigDecimal.valueOf(10_000), 100L)
        );

        Wallet savedWallet = walletRepository.findByMemberId(1L).orElseThrow();

        assertEquals(
                0,
                BigDecimal.valueOf(10_000).compareTo(savedWallet.getAvailableBalance())
        );
        assertEquals(1, savedWallet.getWalletLogs().size());
        assertEquals(1, walletRepository.getSaveCount());
    }

    @Test
    @DisplayName("기존 지갑을 충전하면 기존 잔액에 충전 금액이 더해진다.")
    void topUp_whenWalletExists_increasesBalance() {
        Wallet wallet = Wallet.create(1L);
        wallet.topup(BigDecimal.valueOf(5_000), 10L);
        walletRepository.save(wallet);

        walletRepository.resetSaveCount();

        walletService.topUp(
                new TopUpCommand(1L, BigDecimal.valueOf(3_000), 100L)
        );

        Wallet savedWallet = walletRepository.findByMemberId(1L).orElseThrow();

        assertEquals(
                0,
                BigDecimal.valueOf(8_000).compareTo(savedWallet.getAvailableBalance())
        );
        assertEquals(1, walletRepository.getSaveCount());
    }

    @Test
    void topUp_whenAlreadyProcessedWithSameAmount_doesNothing() {
        final Wallet wallet = saveWalletWithId(100L, BigDecimal.valueOf(10_000));
        walletLogQueryPort.put(
                100L,
                WalletLogType.TOPUP,
                100L,
                BigDecimal.valueOf(3_000)
        );

        walletService.topUp(
                new TopUpCommand(1L, BigDecimal.valueOf(3_000), 100L)
        );

        assertEquals(
                0,
                BigDecimal.valueOf(10_000).compareTo(wallet.getAvailableBalance())
        );
        assertEquals(1, wallet.getWalletLogs().size());
        assertEquals(0, walletRepository.getSaveCount());
    }

    @Test
    void topUp_whenAlreadyProcessedWithDifferentAmount_throwsException() {
        final Wallet wallet = saveWalletWithId(100L, BigDecimal.valueOf(10_000));
        walletLogQueryPort.put(
                100L,
                WalletLogType.TOPUP,
                100L,
                BigDecimal.valueOf(3_000)
        );

        final BusinessException exception = assertThrows(
                BusinessException.class,
                () -> walletService.topUp(
                        new TopUpCommand(1L, BigDecimal.valueOf(4_000), 100L)
                )
        );

        assertEquals(WalletErrorCode.WALLET_LOG_AMOUNT_MISMATCH, exception.getErrorCode());
        assertEquals(
                0,
                BigDecimal.valueOf(10_000).compareTo(wallet.getAvailableBalance())
        );
        assertEquals(0, walletRepository.getSaveCount());
    }

    @Test
    @DisplayName("지갑이 없을 때 홀드하면 WALLET_NOT_FOUND 예외가 발생한다.")
    void hold_whenWalletNotFound_throwsException() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> walletService.hold(
                        new HoldCommand(1L, BigDecimal.valueOf(5_000), 200L)
                )
        );

        assertEquals(WalletErrorCode.WALLET_NOT_FOUND, exception.getErrorCode());
        assertEquals(0, walletRepository.getSaveCount());
    }

    // 헬퍼 함수
    private void saveWalletWithBalance(final BigDecimal amount) {
        Wallet wallet = Wallet.create(1L);
        wallet.topup(amount, 10L);

        walletRepository.save(wallet);
        walletRepository.resetSaveCount();
    }

    @Test
    @DisplayName("기존 지갑을 홀드하면 사용 가능 잔액은 감소하고 홀드 잔액은 증가한다.")
    void hold_whenWalletExists_movesAvailableBalanceToHeldBalance() {
        saveWalletWithBalance(BigDecimal.valueOf(10_000));

        walletService.hold(
                new HoldCommand(1L, BigDecimal.valueOf(6_000), 200L)
        );

        Wallet wallet = walletRepository.findByMemberId(1L).orElseThrow();

        assertEquals(
                0,
                BigDecimal.valueOf(4_000).compareTo(wallet.getAvailableBalance())
        );
        assertEquals(
                0,
                BigDecimal.valueOf(6_000).compareTo(wallet.getHeldBalance())
        );
        assertEquals(1, walletRepository.getSaveCount());
    }

    @Test
    void hold_whenAlreadyProcessedWithSameAmount_doesNothing() {
        final Wallet wallet = saveWalletWithId(100L, BigDecimal.valueOf(10_000));
        walletLogQueryPort.put(
                100L,
                WalletLogType.HOLD,
                200L,
                BigDecimal.valueOf(6_000)
        );

        walletService.hold(
                new HoldCommand(1L, BigDecimal.valueOf(6_000), 200L)
        );

        assertEquals(
                0,
                BigDecimal.valueOf(10_000).compareTo(wallet.getAvailableBalance())
        );
        assertEquals(BigDecimal.ZERO, wallet.getHeldBalance());
        assertEquals(0, walletRepository.getSaveCount());
    }

    @Test
    void hold_whenAlreadyProcessedWithDifferentAmount_throwsException() {
        final Wallet wallet = saveWalletWithId(100L, BigDecimal.valueOf(10_000));
        walletLogQueryPort.put(
                100L,
                WalletLogType.HOLD,
                200L,
                BigDecimal.valueOf(6_000)
        );

        final BusinessException exception = assertThrows(
                BusinessException.class,
                () -> walletService.hold(
                        new HoldCommand(1L, BigDecimal.valueOf(5_000), 200L)
                )
        );

        assertEquals(WalletErrorCode.WALLET_LOG_AMOUNT_MISMATCH, exception.getErrorCode());
        assertEquals(
                0,
                BigDecimal.valueOf(10_000).compareTo(wallet.getAvailableBalance())
        );
        assertEquals(BigDecimal.ZERO, wallet.getHeldBalance());
        assertEquals(0, walletRepository.getSaveCount());
    }

    @Test
    @DisplayName("기존 지갑의 Hold를 해제하면 Hold 잔액은 감소하고 사용 가능 잔액은 증가한다.")
    void release_whenWalletExists_movesHeldBalanceToAvailableBalance() {
        // given
        saveWalletWithHeldAmount(
                100L,
                BigDecimal.valueOf(10_000),
                BigDecimal.valueOf(6_000),
                200L
        );

        walletLogQueryPort.put(
                100L,
                WalletLogType.HOLD,
                200L,
                BigDecimal.valueOf(6_000)
        );

        // when
        walletService.release(
                new ReleaseCommand(1L, BigDecimal.valueOf(6_000), 200L)
        );

        // then
        Wallet savedWallet = walletRepository.findByMemberId(1L).orElseThrow();

        assertEquals(
                0,
                BigDecimal.valueOf(10_000)
                        .compareTo(savedWallet.getAvailableBalance())
        );
        assertEquals(BigDecimal.ZERO, savedWallet.getHeldBalance());
        assertEquals(3, savedWallet.getWalletLogs().size());
        assertEquals(1, walletRepository.getSaveCount());
    }

    @Test
    @DisplayName("기존 Release 금액과 요청 금액이 다르면 예외가 발생한다.")
    void release_whenReleasedAmountDoesNotMatch_throwsException() {
        // given
        final Wallet wallet = saveWalletWithHeldAmount(
                100L,
                BigDecimal.valueOf(10_000),
                BigDecimal.valueOf(6_000),
                200L
        );

        wallet.release(BigDecimal.valueOf(6_000), 200L);
        walletRepository.resetSaveCount();

        walletLogQueryPort.put(
                100L,
                WalletLogType.HOLD,
                200L,
                BigDecimal.valueOf(6_000)
        );
        // 불일치 원장 데이터가 조회된 방어 시나리오
        walletLogQueryPort.put(
                100L,
                WalletLogType.RELEASE,
                200L,
                BigDecimal.valueOf(5_000)
        );

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> walletService.release(
                        new ReleaseCommand(
                                1L,
                                BigDecimal.valueOf(6_000),
                                200L
                        )
                )
        );

        // then
        assertEquals(
                WalletErrorCode.RELEASE_AMOUNT_MISMATCH,
                exception.getErrorCode()
        );
        assertEquals(0, walletRepository.getSaveCount());
    }

    // Hold 로그가 없는 경우
    @Test
    @DisplayName("Hold 로그가 없으면 예외가 발생한다.")
    void release_whenHoldLogNotFound_throwsException() {
        // given
        saveWalletWithHeldAmount(
                100L,
                BigDecimal.valueOf(10_000),
                BigDecimal.valueOf(6_000),
                200L
        );

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> walletService.release(
                        new ReleaseCommand(
                                1L,
                                BigDecimal.valueOf(6_000),
                                200L
                        )
                )
        );

        // then
        assertEquals(WalletErrorCode.HOLD_NOT_FOUND, exception.getErrorCode());
        assertEquals(0, walletRepository.getSaveCount());
    }

    // Hold 금액이 다른 경우
    @Test
    @DisplayName("Hold 금액과 Release 요청 금액이 다르면 예외가 발생한다.")
    void release_whenHoldAmountDoesNotMatch_throwsException() {
        // given
        saveWalletWithHeldAmount(
                100L,
                BigDecimal.valueOf(10_000),
                BigDecimal.valueOf(6_000),
                200L
        );

        walletLogQueryPort.put(
                100L,
                WalletLogType.HOLD,
                200L,
                BigDecimal.valueOf(5_000)
        );

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> walletService.release(
                        new ReleaseCommand(
                                1L,
                                BigDecimal.valueOf(6_000),
                                200L
                        )
                )
        );

        // then
        assertEquals(
                WalletErrorCode.RELEASE_AMOUNT_MISMATCH,
                exception.getErrorCode()
        );
        assertEquals(0, walletRepository.getSaveCount());
    }

    // 동일 금액의 재전송은 no-op
    @Test
    @DisplayName("동일 금액의 Release 요청이 재전송되면 아무 작업도 하지 않는다.")
    void release_whenAlreadyReleasedWithSameAmount_doesNothing() {
        // given
        final Wallet wallet = saveWalletWithHeldAmount(
                100L,
                BigDecimal.valueOf(10_000),
                BigDecimal.valueOf(6_000),
                200L
        );

        wallet.release(BigDecimal.valueOf(6_000), 200L);
        walletRepository.resetSaveCount();

        walletLogQueryPort.put(
                100L,
                WalletLogType.HOLD,
                200L,
                BigDecimal.valueOf(6_000)
        );
        walletLogQueryPort.put(
                100L,
                WalletLogType.RELEASE,
                200L,
                BigDecimal.valueOf(6_000)
        );

        // when
        walletService.release(
                new ReleaseCommand(1L, BigDecimal.valueOf(6_000), 200L)
        );

        // then
        assertEquals(
                0,
                BigDecimal.valueOf(10_000)
                        .compareTo(wallet.getAvailableBalance())
        );
        assertEquals(BigDecimal.ZERO, wallet.getHeldBalance());
        assertEquals(0, walletRepository.getSaveCount());
    }

    @Test
    @DisplayName("Release 원장 저장 중 중복 제약 위반이 발생하면 중복 Release 예외로 변환한다.")
    void release_whenDuplicateReleaseLogSaveFails_throwsException() {
        // given
        final Wallet wallet = saveWalletWithHeldAmount(
                100L,
                BigDecimal.valueOf(10_000),
                BigDecimal.valueOf(6_000),
                200L
        );
        walletLogQueryPort.put(
                wallet.getId(),
                WalletLogType.HOLD,
                200L,
                BigDecimal.valueOf(6_000)
        );
        walletRepository.failOnSave();

        // when
        final BusinessException exception = assertThrows(
                BusinessException.class,
                () -> walletService.release(
                        new ReleaseCommand(
                                1L,
                                BigDecimal.valueOf(6_000),
                                200L
                        )
                )
        );

        // then
        assertEquals(WalletErrorCode.DUPLICATE_RELEASE, exception.getErrorCode());
    }

    @Test
    @DisplayName("기존 지갑에서 사용 가능 잔액으로 결제하면 사용 가능 잔액이 감소한다.")
    void payAvailable_whenWalletExists_decreasesAvailableBalance() {
        saveWalletWithBalance(BigDecimal.valueOf(10_000));

        walletService.payAvailable(
                new PayCommand(1L, BigDecimal.valueOf(7_000), 300L)
        );

        Wallet wallet = walletRepository.findByMemberId(1L).orElseThrow();

        assertEquals(
                0,
                BigDecimal.valueOf(3_000).compareTo(wallet.getAvailableBalance())
        );
        assertEquals(BigDecimal.ZERO, wallet.getHeldBalance());
        assertEquals(1, walletRepository.getSaveCount());
    }

    @Test
    void payAvailable_whenAlreadyProcessedWithSameAmount_doesNothing() {
        final Wallet wallet = saveWalletWithId(100L, BigDecimal.valueOf(10_000));
        wallet.payAvailable(BigDecimal.valueOf(7_000), 300L);
        walletRepository.save(wallet);
        walletRepository.resetSaveCount();
        walletLogQueryPort.put(
                100L,
                WalletLogType.PAYMENT,
                300L,
                BigDecimal.valueOf(7_000)
        );

        walletService.payAvailable(
                new PayCommand(1L, BigDecimal.valueOf(7_000), 300L)
        );

        assertEquals(
                0,
                BigDecimal.valueOf(3_000).compareTo(wallet.getAvailableBalance())
        );
        assertEquals(0, walletRepository.getSaveCount());
    }

    @Test
    void payAvailable_whenAlreadyProcessedWithDifferentAmount_throwsException() {
        final Wallet wallet = saveWalletWithId(100L, BigDecimal.valueOf(10_000));
        wallet.payAvailable(BigDecimal.valueOf(7_000), 300L);
        walletRepository.save(wallet);
        walletRepository.resetSaveCount();
        walletLogQueryPort.put(
                100L,
                WalletLogType.PAYMENT,
                300L,
                BigDecimal.valueOf(7_000)
        );

        final BusinessException exception = assertThrows(
                BusinessException.class,
                () -> walletService.payAvailable(
                        new PayCommand(1L, BigDecimal.valueOf(6_000), 300L)
                )
        );

        assertEquals(WalletErrorCode.WALLET_LOG_AMOUNT_MISMATCH, exception.getErrorCode());
        assertEquals(
                0,
                BigDecimal.valueOf(3_000).compareTo(wallet.getAvailableBalance())
        );
        assertEquals(0, walletRepository.getSaveCount());
    }

    @Test
    @DisplayName("기존 지갑의 홀드 잔액으로 결제하면 홀드 잔액이 감소한다.")
    void payHeld_whenWalletExists_decreasesHeldBalance() {
        Wallet wallet = Wallet.create(1L);
        wallet.topup(BigDecimal.valueOf(10_000), 10L);
        wallet.hold(BigDecimal.valueOf(6_000), 200L);
        walletRepository.save(wallet);
        walletRepository.resetSaveCount();

        walletService.payHeld(
                new PayCommand(1L, BigDecimal.valueOf(6_000), 300L)
        );

        Wallet savedWallet = walletRepository.findByMemberId(1L).orElseThrow();

        assertEquals(
                0,
                BigDecimal.valueOf(4_000).compareTo(savedWallet.getAvailableBalance())
        );
        assertEquals(BigDecimal.ZERO, savedWallet.getHeldBalance());
        assertEquals(1, walletRepository.getSaveCount());
    }

    @Test
    void payHeld_whenAlreadyProcessedWithSameAmount_doesNothing() {
        final Wallet wallet = saveWalletWithHeldAmount(
                100L,
                BigDecimal.valueOf(10_000),
                BigDecimal.valueOf(6_000),
                200L
        );
        wallet.payHeld(BigDecimal.valueOf(6_000), 300L);
        walletRepository.save(wallet);
        walletRepository.resetSaveCount();
        walletLogQueryPort.put(
                100L,
                WalletLogType.PAYMENT,
                300L,
                BigDecimal.valueOf(6_000)
        );

        walletService.payHeld(
                new PayCommand(1L, BigDecimal.valueOf(6_000), 300L)
        );

        assertEquals(
                0,
                BigDecimal.valueOf(4_000).compareTo(wallet.getAvailableBalance())
        );
        assertEquals(BigDecimal.ZERO, wallet.getHeldBalance());
        assertEquals(0, walletRepository.getSaveCount());
    }

    @Test
    void payHeld_whenAlreadyProcessedWithDifferentAmount_throwsException() {
        final Wallet wallet = saveWalletWithHeldAmount(
                100L,
                BigDecimal.valueOf(10_000),
                BigDecimal.valueOf(6_000),
                200L
        );
        wallet.payHeld(BigDecimal.valueOf(6_000), 300L);
        walletRepository.save(wallet);
        walletRepository.resetSaveCount();
        walletLogQueryPort.put(
                100L,
                WalletLogType.PAYMENT,
                300L,
                BigDecimal.valueOf(6_000)
        );

        final BusinessException exception = assertThrows(
                BusinessException.class,
                () -> walletService.payHeld(
                        new PayCommand(1L, BigDecimal.valueOf(5_000), 300L)
                )
        );

        assertEquals(WalletErrorCode.WALLET_LOG_AMOUNT_MISMATCH, exception.getErrorCode());
        assertEquals(
                0,
                BigDecimal.valueOf(4_000).compareTo(wallet.getAvailableBalance())
        );
        assertEquals(BigDecimal.ZERO, wallet.getHeldBalance());
        assertEquals(0, walletRepository.getSaveCount());
    }

    @Test
    @DisplayName("존재하는 지갑을 조회하면 지갑 정보를 반환한다.")
    void getWallet_whenWalletExists_returnsWalletInfo() {
        saveWalletWithBalance(BigDecimal.valueOf(10_000));

        WalletInfo walletInfo = walletService.getWallet(new GetWalletQuery(1L));

        assertEquals(1L, walletInfo.memberId());
        assertEquals(
                0,
                BigDecimal.valueOf(10_000).compareTo(walletInfo.availableBalance())
        );
        assertEquals(BigDecimal.ZERO, walletInfo.heldBalance());
    }

    // 헬퍼 함수 - walletId 가 부여된 지갑을 저장한다.
    private Wallet saveWalletWithId(final Long walletId, final BigDecimal amount) {
        final Wallet wallet = Wallet.create(1L);
        wallet.topup(amount, 10L);

        ReflectionTestUtils.setField(wallet, "id", walletId);

        walletRepository.save(wallet);
        walletRepository.resetSaveCount();

        return wallet;
    }

    private Wallet saveWalletWithHeldAmount(
            final Long walletId,
            final BigDecimal initialBalance,
            final BigDecimal heldAmount,
            final Long offerId
    ) {
        final Wallet wallet = Wallet.create(1L);
        wallet.topup(initialBalance, 10L);
        wallet.hold(heldAmount, offerId);

        ReflectionTestUtils.setField(wallet, "id", walletId);

        walletRepository.save(wallet);
        walletRepository.resetSaveCount();

        return wallet;
    }

    @Test
    @DisplayName("정산 건 하나를 충전하면 사용 가능 잔액이 증가하고 정산 로그가 남는다.")
    void earn_whenWalletExists_increasesAvailableBalance() {
        // given
        saveWalletWithId(100L, BigDecimal.valueOf(10_000));

        // when
        walletService.earn(
                new EarnCommand(100L, BigDecimal.valueOf(3_000), 500L)
        );

        // then
        Wallet wallet = walletRepository.findById(100L).orElseThrow();

        assertEquals(
                0,
                BigDecimal.valueOf(13_000).compareTo(wallet.getAvailableBalance())
        );
        assertEquals(2, wallet.getWalletLogs().size());
        assertEquals(1, walletRepository.getSaveCount());
    }

    @Test
    @DisplayName("존재하지 않는 지갑에 충전하면 WALLET_NOT_FOUND 예외가 발생한다.")
    void earn_whenWalletNotFound_throwsException() {
        // given & when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> walletService.earn(
                        new EarnCommand(100L, BigDecimal.valueOf(3_000), 500L)
                )
        );

        // then
        assertEquals(WalletErrorCode.WALLET_NOT_FOUND, exception.getErrorCode());
        assertEquals(0, walletRepository.getSaveCount());
    }

    @Test
    @DisplayName("정산 건 여러 개를 충전하면 합계만큼 잔액이 증가하고 건별 로그가 남는다.")
    void earnAll_whenWalletExists_increasesBalanceByTotal() {
        // given
        saveWalletWithId(100L, BigDecimal.valueOf(10_000));

        // when
        walletService.earnAll(100L, List.of(
                new EarnCommand(100L, BigDecimal.valueOf(3_000), 500L),
                new EarnCommand(100L, BigDecimal.valueOf(2_500), 501L)
        ));

        // then
        Wallet wallet = walletRepository.findById(100L).orElseThrow();

        assertEquals(
                0,
                BigDecimal.valueOf(15_500).compareTo(wallet.getAvailableBalance())
        );
        assertEquals(3, wallet.getWalletLogs().size());
        assertEquals(1, walletRepository.getSaveCount());
    }

    @Test
    @DisplayName("정산 건 중 금액이 0인 건이 있으면 INVALID_AMOUNT 예외가 발생한다.")
    void earnAll_whenAmountIsZero_throwsException() {
        // given
        saveWalletWithId(100L, BigDecimal.valueOf(10_000));

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> walletService.earnAll(100L, List.of(
                        new EarnCommand(100L, BigDecimal.valueOf(3_000), 500L),
                        new EarnCommand(100L, BigDecimal.ZERO, 501L)
                ))
        );

        // then
        assertEquals(WalletErrorCode.INVALID_AMOUNT, exception.getErrorCode());
        assertEquals(0, walletRepository.getSaveCount());
    }

    @Test
    @DisplayName("존재하지 않는 지갑에 정산 지급하면 WALLET_NOT_FOUND 예외가 발생한다.")
    void earnAll_whenWalletNotFound_throwsException() {
        // given & when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> walletService.earnAll(100L, List.of(
                        new EarnCommand(100L, BigDecimal.valueOf(3_000), 500L)
                ))
        );

        // then
        assertEquals(WalletErrorCode.WALLET_NOT_FOUND, exception.getErrorCode());
        assertEquals(0, walletRepository.getSaveCount());
    }

    @Test
    @DisplayName("회원의 지갑 식별자를 조회하면 walletId 와 memberId 를 반환한다.")
    void getWalletId_whenWalletExists_returnsWalletIdAndMemberId() {
        // given
        saveWalletWithId(100L, BigDecimal.valueOf(10_000));

        // when
        GetWalletInfo getWalletInfo = walletService.getWalletId(1L);

        // then
        assertEquals(100L, getWalletInfo.walletId());
        assertEquals(1L, getWalletInfo.memberId());
    }

    @Test
    @DisplayName("지갑이 없는 회원의 식별자를 조회하면 WALLET_NOT_FOUND 예외가 발생한다.")
    void getWalletId_whenWalletNotFound_throwsException() {
        // given & when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> walletService.getWalletId(1L)
        );

        // then
        assertEquals(WalletErrorCode.WALLET_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("지갑이 없는 회원이면 새 지갑을 생성한다.")
    void saveWallet_whenWalletNotExists_createsWallet() {
        // when
        walletService.saveWallet(1L);

        // then
        Wallet savedWallet = walletRepository.findByMemberId(1L).orElseThrow();
        assertEquals(1L, savedWallet.getMemberId());
    }

    @Test
    @DisplayName("이미 지갑이 있는 회원이면 재요청해도 중복 생성하지 않는다.")
    void saveWallet_whenWalletAlreadyExists_doesNothing() {
        // given
        walletService.saveWallet(1L);
        walletRepository.resetSaveCount();

        // when
        walletService.saveWallet(1L);

        // then
        assertEquals(0, walletRepository.getSaveCount());
    }

    private static class FakeWalletLogQueryPort
            implements WalletLogQueryPort {

        private final Map<LogKey, BigDecimal> logAmounts = new HashMap<>();

        @Override
        public Optional<BigDecimal> findLogAmount(
                final Long walletId,
                final WalletLogType type,
                final Long referenceId
        ) {
            return Optional.ofNullable(
                    logAmounts.get(new LogKey(walletId, type, referenceId))
            );
        }

        void put(
                final Long walletId,
                final WalletLogType type,
                final Long referenceId,
                final BigDecimal amount
        ) {
            logAmounts.put(
                    new LogKey(walletId, type, referenceId),
                    amount
            );
        }

        private record LogKey(
                Long walletId,
                WalletLogType type,
                Long referenceId
        ) {
        }
    }

    private static class FakeWalletRepository implements WalletRepository {

        private final Map<Long, Wallet> wallets = new HashMap<>();
        private int saveCount;
        private boolean failOnSave;

        @Override
        public Optional<Wallet> findById(final Long walletId) {
            return wallets.values().stream()
                    .filter(wallet -> walletId.equals(wallet.getId()))
                    .findFirst();
        }

        @Override
        public Optional<Wallet> findByMemberId(final Long memberId) {
            return Optional.ofNullable(wallets.get(memberId));
        }

        @Override
        public Wallet save(final Wallet wallet) {
            if (failOnSave) {
                throw new DataIntegrityViolationException("duplicate release");
            }

            wallets.put(wallet.getMemberId(), wallet);
            saveCount++;
            return wallet;
        }

        public int getSaveCount() {
            return saveCount;
        }

        public void resetSaveCount() {
            saveCount = 0;
        }

        void failOnSave() {
            failOnSave = true;
        }
    }
}
