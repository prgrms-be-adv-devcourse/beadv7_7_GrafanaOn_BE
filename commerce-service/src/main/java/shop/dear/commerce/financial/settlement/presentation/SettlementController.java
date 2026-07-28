package shop.dear.commerce.financial.settlement.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.commerce.financial.settlement.application.SettlementService;
import shop.dear.commerce.financial.settlement.presentation.dto.response.NetAmountResponse;
import shop.dear.common.response.ApiResponse;

import java.time.YearMonth;

import static shop.dear.common.response.ApiResponse.successWithData;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlement")
public class SettlementController {

	private final SettlementService settlementService;

	//정산예정금액 조회
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<NetAmountResponse>> getNetAmount(Long walletId, YearMonth targetMonth) {

		NetAmountResponse netAmountResponse = NetAmountResponse.from(settlementService.getSettlementAmount(walletId, targetMonth));

		return ResponseEntity.ok(successWithData(netAmountResponse));
	}
}
