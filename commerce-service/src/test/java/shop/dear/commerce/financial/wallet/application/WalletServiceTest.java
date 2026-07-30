package shop.dear.commerce.financial.wallet.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import shop.dear.commerce.financial.wallet.application.dto.*;
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

    @BeforeEach
    void setUp() {
        walletRepository = new FakeWalletRepository();
        walletService = new WalletService(walletRepository);
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
    @DisplayName("기존 지갑의 홀드를 해제하면 홀드 잔액은 감소하고 사용 가능 잔액은 증가한다.")
    void release_whenWalletExists_movesHeldBalanceToAvailableBalance() {
        Wallet wallet = Wallet.create(1L);
        wallet.topup(BigDecimal.valueOf(10_000), 10L);
        wallet.hold(BigDecimal.valueOf(6_000), 200L);
        walletRepository.save(wallet);
        walletRepository.resetSaveCount();

        walletService.release(
                new ReleaseCommand(1L, BigDecimal.valueOf(6_000), 200L)
        );

        Wallet savedWallet = walletRepository.findByMemberId(1L).orElseThrow();

        assertEquals(
                0,
                BigDecimal.valueOf(10_000).compareTo(savedWallet.getAvailableBalance())
        );
        assertEquals(BigDecimal.ZERO, savedWallet.getHeldBalance());
        assertEquals(1, walletRepository.getSaveCount());
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

    private static class FakeWalletRepository implements WalletRepository {

        private final Map<Long, Wallet> wallets = new HashMap<>();
        private int saveCount;

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
    }
}
