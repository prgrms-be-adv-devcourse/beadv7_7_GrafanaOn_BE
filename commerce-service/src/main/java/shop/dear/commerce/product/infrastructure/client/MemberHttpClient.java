package shop.dear.commerce.product.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import shop.dear.commerce.product.application.dto.external.ExistsMember;
import shop.dear.commerce.product.application.dto.external.IsSeller;
import shop.dear.commerce.product.application.dto.external.MemberProfile;
import shop.dear.commerce.product.application.port.MemberPort;
import shop.dear.commerce.product.infrastructure.client.dto.ExistsMemberApiData;
import shop.dear.commerce.product.infrastructure.client.dto.IsSellerApiData;
import shop.dear.commerce.product.infrastructure.client.dto.MemberProfileApiData;
import shop.dear.common.response.ApiResponse;

@Slf4j
@Component("productMemberHttpClient")
public class MemberHttpClient implements MemberPort {

    private final RestClient memberRestClient;

    public MemberHttpClient(@Qualifier("productMemberRestClient") final RestClient memberRestClient) {
        this.memberRestClient = memberRestClient;
    }

    @Override
    public ExistsMember existsMember(final Long memberId) {
        log.info("[MemberHttpClient] member - 사용자 검증 요청. memberId={}", memberId);

        final ApiResponse<ExistsMemberApiData> body = memberRestClient.get()
            .uri("/internal/members")
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });

        final ExistsMemberApiData data = body.getData();

        log.info("[MemberHttpClient] member - 사용자 검증 요청 성공. memberId={}", memberId);

        return new ExistsMember(data.exists());
    }

    @Override
    public MemberProfile getMemberProfile(final Long memberId) {
        log.info("[MemberHttpClient] member - 프로필 조회 요청. memberId={}", memberId);

        final ApiResponse<MemberProfileApiData> body = memberRestClient.get()
            .uri("/api/members/profile?memberId=" + memberId)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });

        final MemberProfileApiData data = body.getData();

        log.info("[MemberHttpClient] member - 프로필 조회 요청 성공. memberId={}", memberId);

        return new MemberProfile(
            data.name(),
            data.nickname(),
            data.email(),
            data.defaultShippingAddress(),
            data.phoneNumber()
        );
    }

    @Override
    public IsSeller isSeller(final Long memberId) {
        log.info("[MemberHttpClient] member - 판매자 등록 여부 조회 요청. memberId={}", memberId);

        final ApiResponse<IsSellerApiData> body = memberRestClient.get()
            .uri("/internal/members/seller")
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });

        final IsSellerApiData data = body.getData();

        log.info("[MemberHttpClient] member - 판매자 등록 여부 조회 요청 성공. memberId={} isSeller={}", memberId, data.isSeller());

        return new IsSeller(data.isSeller());
    }
}
