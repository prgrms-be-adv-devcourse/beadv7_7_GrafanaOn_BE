package shop.dear.commerce.order.purchase.infrastructure.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import shop.dear.commerce.order.purchase.application.port.MemberPort;
import shop.dear.common.exception.BusinessException;

import static shop.dear.commerce.order.purchase.domain.exception.PurchaseErrorCode.MEMBER_NOT_FOUND;

@Component("purchaseMemberHttpClient")
public class MemberHttpClient implements MemberPort {

    private static final String MEMBER_URI = "/internal/members";

    private final RestClient memberRestClient;

    public MemberHttpClient(@Qualifier("memberRestClient") final RestClient memberRestClient) {
        this.memberRestClient = memberRestClient;
    }

    @Override
    public void validateMemberExists(final Long memberId) {
        try {
            memberRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path(MEMBER_URI)
                            .queryParam("memberId", memberId)
                            .build())
                    .retrieve()
                    .toBodilessEntity();
        } catch (final RestClientResponseException e) {
            throw new BusinessException(MEMBER_NOT_FOUND);
        }
    }
}
