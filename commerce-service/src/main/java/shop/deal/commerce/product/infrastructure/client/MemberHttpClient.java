package shop.deal.commerce.product.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import shop.deal.commerce.product.application.dto.external.MemberProfile;
import shop.deal.commerce.product.application.port.MemberPort;
import shop.deal.commerce.product.infrastructure.client.dto.MemberProfileApiData;
import shop.deal.common.response.ApiResponse;

@RequiredArgsConstructor
@Component
public class MemberHttpClient implements MemberPort {

    private final RestClient productRestClient;

    @Override
    public MemberProfile getMemberProfile(final Long memberId) {
        final ApiResponse<MemberProfileApiData> body = productRestClient.get()
            .uri("/api/members/profile?memberId=" + memberId)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });

        final MemberProfileApiData data = body.getData();

        return new MemberProfile(
            data.name(),
            data.nickname(),
            data.email(),
            data.defaultShippingAddress(),
            data.phoneNumber()
        );
    }
}
