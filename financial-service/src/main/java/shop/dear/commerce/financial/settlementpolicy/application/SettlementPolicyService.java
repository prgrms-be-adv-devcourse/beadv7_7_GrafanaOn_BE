package shop.dear.commerce.financial.settlementpolicy.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.commerce.financial.settlementpolicy.domain.model.SettlementPolicy;
import shop.dear.commerce.financial.settlementpolicy.domain.repository.SettlementPolicyRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementPolicyService {

    private static final BigDecimal DEFAULT_FEE_RATE = new BigDecimal("0.10");
    private static final BigDecimal DEFAULT_FIXED_FEE = BigDecimal.ZERO;

    private final SettlementPolicyRepository settlementPolicyRepository;

    @Transactional
    public SettlementPolicy getOrCreateDefaultPolicy() {
        return settlementPolicyRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> settlementPolicyRepository.save(
                        SettlementPolicy.create(
                                DEFAULT_FEE_RATE,
                                DEFAULT_FIXED_FEE
                        )
                ));
    }
}
