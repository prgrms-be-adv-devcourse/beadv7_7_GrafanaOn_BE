package shop.dear.commerce.financial.wallet.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shop.dear.commerce.financial.wallet.application.dto.*;
import shop.dear.commerce.financial.wallet.domain.exception.WalletErrorCode;
import shop.dear.commerce.financial.wallet.domain.model.Wallet;
import shop.dear.commerce.financial.wallet.domain.repository.WalletRepository;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;
import java.util.HashMap;
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

    private static class FakeWalletRepository implements WalletRepository {

        private final Map<Long, Wallet> wallets = new HashMap<>();
        private int saveCount;

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
