package shop.dear.commerce.financial.settlement.presentation;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.commerce.financial.settlement.application.SettlementService;
import shop.dear.commerce.financial.settlement.presentation.dto.response.NetAmountResponse;
import shop.dear.commerce.financial.settlement.presentation.dto.response.SettlementResponse;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static shop.dear.common.response.ApiResponse.successWithData;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlements")
public class SettlementController {

	private final SettlementService settlementService;

	//특정월의 정산예정금액 조회 (전월 1일 ~ 말일)
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<NetAmountResponse>> getNetAmount(@AuthUser Long memberId, @NotNull YearMonth targetMonth) {

		//memverId검증로직추가

		NetAmountResponse netAmountResponse = NetAmountResponse.from(settlementService.getNetAmount(targetMonth));

		return ResponseEntity.ok(successWithData(netAmountResponse));
	}

	//특정기간 동안의 정산완료 이력 조회
	@GetMapping("/me/history")
	public ResponseEntity<ApiResponse<List<SettlementResponse>>> getHistory(
		@AuthUser Long memberId,
		@NotNull LocalDateTime startDate,
		@NotNull LocalDateTime endDate
	) {
		//memverId검증로직추가

		List<SettlementResponse> history = settlementService.getHistory(
				startDate,
				endDate
			)
			.stream()
			.map(SettlementResponse::from)
			.toList();
		
		return ResponseEntity.ok(successWithData(history));
	}
}
