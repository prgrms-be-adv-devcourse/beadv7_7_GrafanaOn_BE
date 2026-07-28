package shop.dear.commerce.financial.settlement.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.commerce.financial.settlement.application.SettlementService;
import shop.dear.commerce.financial.settlement.presentation.dto.response.NetAmountResponse;
import shop.dear.commerce.financial.settlement.presentation.dto.response.SettlementResponse;
import shop.dear.common.response.ApiResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import static shop.dear.common.response.ApiResponse.successWithData;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlement")
public class SettlementController {

	private final SettlementService settlementService;

	//정산예정금액 조회
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<NetAmountResponse>> getNetAmount(Long memberId, YearMonth targetMonth) {

		NetAmountResponse netAmountResponse = NetAmountResponse.from(settlementService.getSettlementAmount(memberId, targetMonth));

		return ResponseEntity.ok(successWithData(netAmountResponse));
	}

	@GetMapping("/me/history")
	public ResponseEntity<ApiResponse<List<SettlementResponse>>> getHistory(Long memberId, LocalDateTime startDate, LocalDateTime endDate) {

		List<SettlementResponse> settlementResponse = settlementService.getSettlements(
			memberId,
			startDate,
			endDate
		).stream()
		.map(SettlementResponse::from)
		.toList();
		
		return ResponseEntity.ok(successWithData(settlementResponse));
	}
}
