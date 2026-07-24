package shop.dear.commerce.financial.wallet.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shop.dear.commerce.financial.wallet.domain.costant.WalletLogType;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class WalletTest {
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = Wallet.create(1L);
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getBalance()));
    }

    @Test
    @DisplayName("충전하면 잔액이 증가하고 TOPUP 로그가 생성된다")
    void topup_success() {
        WalletLog log = wallet.topup(BigDecimal.valueOf(10000), 100L);

        assertEquals(0, BigDecimal.valueOf(10000).compareTo(wallet.getBalance()));
        assertEquals(WalletLogType.TOPUP, log.getType());
        assertEquals(100L, log.getReferenceId());
    }

    @Test
    @DisplayName("잔액이 충분하면 홀드에 성공하고 잔액이 차감된다")
    void hold_success() {
        wallet.topup(BigDecimal.valueOf(10000), 100L);

        WalletLog log = wallet.hold(BigDecimal.valueOf(6000), 200L);

        assertEquals(0, BigDecimal.valueOf(4000).compareTo(wallet.getBalance()));
        assertEquals(WalletLogType.HOLD, log.getType());
        assertEquals(200L, log.getReferenceId());
    }

    @Test
    @DisplayName("잔액보다 큰 금액을 홀드하려 하면 예외가 발생한다")
    void hold_insufficientBalance_throwsException() {
        wallet.topup(BigDecimal.valueOf(1000), 100L);

        assertThrows(BusinessException.class,
                () -> wallet.hold(BigDecimal.valueOf(5000), 200L));
    }

    @Test
    @DisplayName("홀드를 릴리즈하면 잔액이 복원된다")
    void release_success() {
        wallet.topup(BigDecimal.valueOf(10000), 100L);
        wallet.hold(BigDecimal.valueOf(6000), 200L);

        WalletLog log = wallet.release(BigDecimal.valueOf(6000), 200L);

        assertEquals(0, BigDecimal.valueOf(10000).compareTo(wallet.getBalance()));
        assertEquals(WalletLogType.RELEASE, log.getType());
        assertEquals(200L, log.getReferenceId());
    }

    @Test
    @DisplayName("잔액이 충분하면 차감(pay)에 성공한다")
    void pay_success() {
        wallet.topup(BigDecimal.valueOf(10000), 100L);

        WalletLog log = wallet.pay(BigDecimal.valueOf(7000), 300L);

        assertEquals(0, BigDecimal.valueOf(3000).compareTo(wallet.getBalance()));
        assertEquals(WalletLogType.PAYMENT, log.getType());
        assertEquals(300L, log.getReferenceId());
    }

    @Test
    @DisplayName("잔액보다 큰 금액을 차감하려 하면 예외가 발생한다")
    void pay_insufficientBalance_throwsException() {
        wallet.topup(BigDecimal.valueOf(1000), 100L);

        assertThrows(BusinessException.class,
                () -> wallet.pay(BigDecimal.valueOf(5000), 300L));
    }

    @Test
    @DisplayName("판매 대금이 적립되면 잔액이 증가하고 PROCEEDS 로그가 생성된다")
    void earn_success() {
        WalletLog log = wallet.earn(BigDecimal.valueOf(50000), 400L);

        assertEquals(0, BigDecimal.valueOf(50000).compareTo(wallet.getBalance()));
        assertEquals(WalletLogType.PROCEEDS, log.getType());
        assertEquals(400L, log.getReferenceId());
    }

    @Test
    @DisplayName("잔액이 충분하면 정산 요청 시 차감에 성공한다")
    void settle_success() {
        wallet.earn(BigDecimal.valueOf(50000), 400L);

        WalletLog log = wallet.settle(BigDecimal.valueOf(30000), 500L);

        assertEquals(0, BigDecimal.valueOf(20000).compareTo(wallet.getBalance()));
        assertEquals(WalletLogType.SETTLEMENT, log.getType());
        assertEquals(500L, log.getReferenceId());
    }

    @Test
    @DisplayName("잔액보다 큰 금액을 정산 요청하면 예외가 발생한다")
    void settle_insufficientBalance_throwsException() {
        wallet.earn(BigDecimal.valueOf(10000), 400L);

        assertThrows(BusinessException.class,
                () -> wallet.settle(BigDecimal.valueOf(50000), 500L));
    }
}
