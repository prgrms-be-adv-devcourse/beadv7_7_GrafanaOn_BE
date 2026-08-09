package shop.dear.commerce.financial.settlementpolicy.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.commerce.financial.settlementpolicy.domain.model.SettlementPolicy;
import shop.dear.commerce.financial.settlementpolicy.domain.repository.SettlementPolicyRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SettlementPolicyServiceTest {

    @Mock
    private SettlementPolicyRepository settlementPolicyRepository;

    @InjectMocks
    private SettlementPolicyService settlementPolicyService;

    @Test
    void getOrCreateDefaultPolicy_returnsExistingPolicy() {
        // given
        final SettlementPolicy existingPolicy = SettlementPolicy.create(
                new BigDecimal("0.05"),
                new BigDecimal("100.00")
        );

        given(settlementPolicyRepository.findFirstByOrderByIdAsc())
                .willReturn(Optional.of(existingPolicy));

        // when
        final SettlementPolicy result =
                settlementPolicyService.getOrCreateDefaultPolicy();

        // then
        assertThat(result).isSameAs(existingPolicy);
        verify(settlementPolicyRepository, never()).save(any());
    }

    @Test
    void getOrCreateDefaultPolicy_createsTenPercentPolicyWhenMissing() {
        // given
        given(settlementPolicyRepository.findFirstByOrderByIdAsc())
                .willReturn(Optional.empty());

        given(settlementPolicyRepository.save(any(SettlementPolicy.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        final SettlementPolicy result =
                settlementPolicyService.getOrCreateDefaultPolicy();

        // then
        final ArgumentCaptor<SettlementPolicy> captor =
                ArgumentCaptor.forClass(SettlementPolicy.class);

        verify(settlementPolicyRepository).save(captor.capture());

        final SettlementPolicy savedPolicy = captor.getValue();

        assertThat(savedPolicy.getFeeRate())
                .isEqualByComparingTo("0.10");
        assertThat(savedPolicy.getFixedFee())
                .isEqualByComparingTo("0.00");
        assertThat(result).isSameAs(savedPolicy);
    }
}
