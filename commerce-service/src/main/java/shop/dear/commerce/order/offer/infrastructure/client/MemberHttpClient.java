package shop.dear.commerce.order.offer.infrastructure.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import shop.dear.commerce.order.offer.application.port.MemberPort;
import shop.dear.common.exception.BusinessException;
import shop.dear.common.response.ApiResponse;

import static shop.dear.commerce.order.offer.domain.exception.OfferErrorCode.OFFER_MEMBER_NOT_FOUND;

@Component("offerMemberHttpClient")
public class MemberHttpClient implements MemberPort {

    private static final String MEMBER_URI = "/internal/members";

    private record ExistsMemberApiData(boolean exists) {
    }

    private final RestClient memberRestClient;

    public MemberHttpClient(@Qualifier("offerMemberRestClient") final RestClient memberRestClient) {
        this.memberRestClient = memberRestClient;
    }

    @Override
    public void validateMemberExists() {
        try {
            final ApiResponse<ExistsMemberApiData> response = memberRestClient.get()
                    .uri(MEMBER_URI)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.getData() == null || !response.getData().exists()) {
                throw new BusinessException(OFFER_MEMBER_NOT_FOUND);
            }
        } catch (final RestClientResponseException e) {
            throw new BusinessException(OFFER_MEMBER_NOT_FOUND);
        }
    }
}
