package shop.dear.commerce.financial.wallet.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shop.dear.commerce.financial.wallet.domain.costant.WalletLogType;
import shop.dear.commerce.financial.wallet.domain.exception.WalletErrorCode;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WalletTest {
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = Wallet.create(1L);
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getAvailableBalance()));
    }

    private WalletLog lastLog() {
        List<WalletLog> logs = wallet.getWalletLogs();
        return logs.get(logs.size() - 1);
    }

    @Test
    @DisplayName("충전하면 잔액이 증가하고 TOPUP 로그가 생성된다")
    void topup_success() {
        wallet.topup(BigDecimal.valueOf(10000), 100L);

        assertEquals(0, BigDecimal.valueOf(10000).compareTo(wallet.getAvailableBalance()));
        assertEquals(WalletLogType.TOPUP, lastLog().getType());
        assertEquals(100L, lastLog().getReferenceId());
    }

    @Test
    @DisplayName("잔액이 충분하면 홀드에 성공하고 잔액이 차감된다")
    void hold_success() {
        wallet.topup(BigDecimal.valueOf(10000), 100L);

        wallet.hold(BigDecimal.valueOf(6000), 200L);

        assertEquals(0, BigDecimal.valueOf(4000).compareTo(wallet.getAvailableBalance()));
        assertEquals(0, BigDecimal.valueOf(6000).compareTo(wallet.getHeldBalance()));
        assertEquals(WalletLogType.HOLD, lastLog().getType());
        assertEquals(200L, lastLog().getReferenceId());
    }

    @Test
    @DisplayName("잔액보다 큰 금액을 홀드하려 하면 예외가 발생한다")
    void hold_insufficientBalance_throwsException() {
        wallet.topup(BigDecimal.valueOf(1000), 100L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wallet.hold(BigDecimal.valueOf(5000), 200L));
        assertEquals(WalletErrorCode.INSUFFICIENT_BALANCE, e.getErrorCode());
    }

    @Test
    @DisplayName("홀드를 릴리즈하면 잔액이 복원된다")
    void release_success() {
        wallet.topup(BigDecimal.valueOf(10000), 100L);
        wallet.hold(BigDecimal.valueOf(6000), 200L);

        wallet.release(BigDecimal.valueOf(6000), 200L);

        assertEquals(0, BigDecimal.valueOf(10000).compareTo(wallet.getAvailableBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getHeldBalance()));
        assertEquals(WalletLogType.RELEASE, lastLog().getType());
        assertEquals(200L, lastLog().getReferenceId());
    }

    @Test
    @DisplayName("잔액이 충분하면 차감(pay)에 성공한다")
    void pay_success() {
        wallet.topup(BigDecimal.valueOf(10000), 100L);

        wallet.payAvailable(BigDecimal.valueOf(7000), 300L);

        assertEquals(0, BigDecimal.valueOf(3000).compareTo(wallet.getAvailableBalance()));
        assertEquals(WalletLogType.PAYMENT, lastLog().getType());
        assertEquals(300L, lastLog().getReferenceId());
    }

    @Test
    @DisplayName("잔액보다 큰 금액을 차감하려 하면 예외가 발생한다")
    void pay_insufficientBalance_throwsException() {
        wallet.topup(BigDecimal.valueOf(1000), 100L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wallet.payAvailable(BigDecimal.valueOf(5000), 300L));
        assertEquals(WalletErrorCode.INSUFFICIENT_BALANCE, e.getErrorCode());
    }

    @Test
    @DisplayName("판매 대금이 적립되면 잔액이 증가하고 PROCEEDS 로그가 생성된다")
    void earn_success() {
        wallet.earn(BigDecimal.valueOf(50000), 400L);

        assertEquals(0, BigDecimal.valueOf(50000).compareTo(wallet.getAvailableBalance()));
        assertEquals(WalletLogType.PROCEEDS, lastLog().getType());
        assertEquals(400L, lastLog().getReferenceId());
    }

    @Test
    @DisplayName("잔액이 충분하면 정산 요청 시 차감에 성공한다")
    void settle_success() {
        wallet.earn(BigDecimal.valueOf(50000), 400L);

        wallet.settle(BigDecimal.valueOf(30000), 500L);

        assertEquals(0, BigDecimal.valueOf(20000).compareTo(wallet.getAvailableBalance()));
        assertEquals(WalletLogType.SETTLEMENT, lastLog().getType());
        assertEquals(500L, lastLog().getReferenceId());
    }

    @Test
    @DisplayName("잔액보다 큰 금액을 정산 요청하면 예외가 발생한다")
    void settle_insufficientBalance_throwsException() {
        wallet.earn(BigDecimal.valueOf(10000), 400L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wallet.settle(BigDecimal.valueOf(50000), 500L));
        assertEquals(WalletErrorCode.INSUFFICIENT_BALANCE, e.getErrorCode());
    }

    // 금액 검증

    @Test
    @DisplayName("금액이 null이면 INVALID_AMOUNT 예외가 발생한다")
    void pay_nullAmount_throwsInvalidAmount() {
        wallet.topup(BigDecimal.valueOf(1000), 100L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wallet.payAvailable(null, 300L));
        assertEquals(WalletErrorCode.INVALID_AMOUNT, e.getErrorCode());
    }

    @Test
    @DisplayName("금액이 0이면 INVALID_AMOUNT 예외가 발생한다")
    void pay_zeroAmount_throwsInvalidAmount() {
        wallet.topup(BigDecimal.valueOf(1000), 100L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wallet.payAvailable(BigDecimal.ZERO, 300L));
        assertEquals(WalletErrorCode.INVALID_AMOUNT, e.getErrorCode());
    }

    @Test
    @DisplayName("음수 금액으로 pay하면 INVALID_AMOUNT 예외가 발생하고 잔액이 변하지 않는다")
    void pay_negativeAmount_throwsInvalidAmount() {
        wallet.topup(BigDecimal.valueOf(1000), 100L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wallet.payAvailable(BigDecimal.valueOf(-100), 300L));
        assertEquals(WalletErrorCode.INVALID_AMOUNT, e.getErrorCode());
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(wallet.getAvailableBalance()));
    }

    @Test
    @DisplayName("소수점 2자리를 초과하면 INVALID_AMOUNT 예외가 발생한다")
    void pay_scaleOverTwo_throwsInvalidAmount() {
        wallet.topup(BigDecimal.valueOf(1000), 100L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wallet.payAvailable(new BigDecimal("100.123"), 300L));
        assertEquals(WalletErrorCode.INVALID_AMOUNT, e.getErrorCode());
    }

    @Test
    @DisplayName("referenceId가 null이면 INVALID_REFERENCE_ID 예외가 발생한다")
    void pay_nullReferenceId_throwsInvalidReferenceId() {
        wallet.topup(BigDecimal.valueOf(1000), 100L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wallet.payAvailable(BigDecimal.valueOf(100), null));
        assertEquals(WalletErrorCode.INVALID_REFERENCE_ID, e.getErrorCode());
    }

    @Test
    @DisplayName("음수 금액으로 topup하면 예외가 발생하고 잔액이 변하지 않는다")
    void topup_negativeAmount_throwsInvalidAmount() {
        wallet.topup(BigDecimal.valueOf(1000), 100L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wallet.topup(BigDecimal.valueOf(-500), 200L));
        assertEquals(WalletErrorCode.INVALID_AMOUNT, e.getErrorCode());
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(wallet.getAvailableBalance()));
    }

    @Test
    @DisplayName("음수 금액으로 hold하면 INVALID_AMOUNT 예외가 발생한다")
    void hold_negativeAmount_throwsInvalidAmount() {
        wallet.topup(BigDecimal.valueOf(1000), 100L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wallet.hold(BigDecimal.valueOf(-100), 200L));
        assertEquals(WalletErrorCode.INVALID_AMOUNT, e.getErrorCode());
    }

    @Test
    @DisplayName("음수 금액으로 release하면 INVALID_AMOUNT 예외가 발생한다")
    void release_negativeAmount_throwsInvalidAmount() {
        wallet.topup(BigDecimal.valueOf(1000), 100L);
        wallet.hold(BigDecimal.valueOf(300), 200L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wallet.release(BigDecimal.valueOf(-100), 200L));
        assertEquals(WalletErrorCode.INVALID_AMOUNT, e.getErrorCode());
    }

    @Test
    @DisplayName("음수 금액으로 settle하면 INVALID_AMOUNT 예외가 발생한다")
    void settle_negativeAmount_throwsInvalidAmount() {
        wallet.earn(BigDecimal.valueOf(10000), 400L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wallet.settle(BigDecimal.valueOf(-100), 500L));
        assertEquals(WalletErrorCode.INVALID_AMOUNT, e.getErrorCode());
    }

    @Test
    @DisplayName("음수 금액으로 earn하면 INVALID_AMOUNT 예외가 발생한다")
    void earn_negativeAmount_throwsInvalidAmount() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> wallet.earn(BigDecimal.valueOf(-100), 400L));
        assertEquals(WalletErrorCode.INVALID_AMOUNT, e.getErrorCode());
    }

    // hold/release 무결성

    @Test
    @DisplayName("release 요청 금액이 heldBalance를 초과하면 INSUFFICIENT_HELD_BALANCE 예외가 발생한다")
    void release_exceedsHeldBalance_throwsInsufficientHeldBalance() {
        wallet.topup(BigDecimal.valueOf(1000), 100L);
        wallet.hold(BigDecimal.valueOf(300), 200L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wallet.release(BigDecimal.valueOf(500), 200L));
        assertEquals(WalletErrorCode.INSUFFICIENT_HELD_BALANCE, e.getErrorCode());
    }

    @Test
    @DisplayName("홀드된 금액으로 결제하면 heldBalance만 차감되고 availableBalance는 변하지 않으며 PAYMENT 로그가 생성된다")
    void payHeld_success() {
        wallet.topup(BigDecimal.valueOf(10000), 100L);
        wallet.hold(BigDecimal.valueOf(6000), 200L);

        wallet.payHeld(BigDecimal.valueOf(6000), 200L);

        assertEquals(0, BigDecimal.valueOf(4000).compareTo(wallet.getAvailableBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getHeldBalance()));
        assertEquals(WalletLogType.PAYMENT, lastLog().getType());
    }

    @Test
    @DisplayName("홀드된 금액보다 큰 금액을 payHeld 하면 예외가 발생한다")
    void payHeld_exceedsHeldBalance_throwsException() {
        wallet.topup(BigDecimal.valueOf(10000), 100L);
        wallet.hold(BigDecimal.valueOf(3000), 200L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> wallet.payHeld(BigDecimal.valueOf(5000), 200L));
        assertEquals(WalletErrorCode.INSUFFICIENT_HELD_BALANCE, e.getErrorCode());
    }

    // 읽기 전용 컬렉션 테스트

    @Test
    @DisplayName("walletLogs는 외부에서 수정할 수 없다")
    void walletLogs_isUnmodifiable() {
        wallet.topup(BigDecimal.valueOf(1000), 100L);

        assertThrows(UnsupportedOperationException.class,
                () -> wallet.getWalletLogs().clear());
    }
}