package shop.dear.commerce.financial.settlement.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.commerce.financial.settlement.domain.constant.SettlementBatchStatus;
import shop.dear.commerce.financial.settlement.domain.exception.SettlementErrorCode;
import shop.dear.common.exception.BusinessException;

import java.math.BigDecimal;
import java.time.YearMonth;

@Entity
@Table(name = "settlement_batch",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_settlement_batch_period_wallet",
			columnNames = {"period", "wallet_id"}
		)
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementBatch {	//집계용이므로 baseEntity를 상속하지 않는다. (insert/ update 필드가 필요하지 않음)

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "period", nullable = false, length = 6)
	private String period;

	@Column(name = "wallet_id", nullable = false)
	private Long walletId;

	// 누적된 건별 정산 수. 금액만으로는 누락과 상계를 구분할 수 없어 검증에 함께 쓴다.
	@Column(name = "settlement_count", nullable = false)
	private int settlementCount;

	@Column(name = "gross_amount", precision = 15, scale = 2, nullable = false)
	private BigDecimal grossAmount;

	@Column(name = "fee_amount", precision = 15, scale = 2, nullable = false)
	private BigDecimal feeAmount;

	@Column(name = "net_amount", precision = 15, scale = 2, nullable = false)
	private BigDecimal netAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "state", nullable = false, length = 30)
	private SettlementBatchStatus state;

	private SettlementBatch(final YearMonth period, final Long walletId) {
		this.period = period.toString();
		this.walletId = walletId;
		this.settlementCount = 0;
		this.grossAmount = BigDecimal.ZERO;
		this.feeAmount = BigDecimal.ZERO;
		this.netAmount = BigDecimal.ZERO;
		this.state = SettlementBatchStatus.PENDING;
	}

	public static SettlementBatch create(final YearMonth period, final Long walletId) {
		return new SettlementBatch(period, walletId);
	}

	// 정산 건을 집계에 누적한다.
	public void accumulate(BigDecimal grossAmount, BigDecimal feeAmount, BigDecimal netAmount) {
		validateState(SettlementBatchStatus.PENDING);

		this.settlementCount += 1;
		this.grossAmount = this.grossAmount.add(grossAmount);
		this.feeAmount = this.feeAmount.add(feeAmount);
		this.netAmount = this.netAmount.add(netAmount);
	}

	public void cancle(BigDecimal grossAmount, BigDecimal feeAmount, BigDecimal netAmount) {
		validateState(SettlementBatchStatus.PENDING);

		this.settlementCount -= 1;
		this.grossAmount = this.grossAmount.subtract(grossAmount);
		this.feeAmount = this.feeAmount.subtract(feeAmount);
		this.netAmount = this.netAmount.subtract(netAmount);
	}

	public void close() {
		validateState(SettlementBatchStatus.PENDING);

		this.state = SettlementBatchStatus.CLOSE;
	}

	public boolean matches(
		final int settlementCount,
		final BigDecimal grossAmount,
		final BigDecimal feeAmount,
		final BigDecimal netAmount
	) {
		return this.settlementCount == settlementCount
			&& this.grossAmount.compareTo(grossAmount) == 0
			&& this.feeAmount.compareTo(feeAmount) == 0
			&& this.netAmount.compareTo(netAmount) == 0;
	}

	public void paid() {
		validateState(SettlementBatchStatus.CLOSE);

		this.state = SettlementBatchStatus.COMPLETED;
	}

	public void revalidate() {
		validateState(SettlementBatchStatus.CLOSE);

		this.state = SettlementBatchStatus.FAILED;
	}

	private void validateState(final SettlementBatchStatus expected) {
		if (this.state != expected) {
			throw new BusinessException(SettlementErrorCode.INVALID_SETTLEMENT_BATCH_STATUS_TRANSITION);
		}
	}
}
