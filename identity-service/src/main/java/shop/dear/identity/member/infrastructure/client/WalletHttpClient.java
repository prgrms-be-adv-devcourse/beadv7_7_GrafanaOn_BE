package shop.dear.identity.member.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import shop.dear.common.exception.ServiceUnavailableException;
import shop.dear.identity.member.application.port.WalletPort;
import shop.dear.identity.member.domain.exception.MemberErrorCode;
import shop.dear.identity.member.infrastructure.client.dto.WalletApiRequest;

/**
 * 회원가입 시 commerce에 지갑 생성을 요청합니다.
 *
 * 부하 테스트에서 이 호출이 지연되면 회원가입 성공률이 9%까지 떨어졌습니다.
 * 차단기를 두어 반복 실패 시 호출 자체를 보내지 않고 즉시 실패하도록 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletHttpClient implements WalletPort {

    private static final String CIRCUIT_BREAKER_NAME = "commerceWallet";

    private final RestClient walletRestClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Override
    public void createWallet(final Long memberId) {
        circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME).run(() ->
                callCreateWallet(memberId), this::createWalletFallback);
    }

    // 기존 createWallet 코드를 callCreateWallet으로 옮겼습니다.
    private Void callCreateWallet(final Long memberId) {
        walletRestClient.post()
                .uri("/internal/deposits")
                .body(new WalletApiRequest(memberId))
                .retrieve()
                .toBodilessEntity();

        return null;
    }

    /**
     * 두 가지 경로를 통해 이 메서드에 접근할 수 있다.
     * 1. 호출이 실제로 실패하거나 타임아웃 발생 -> 서킷브레이커가 실패로 기록
     * 2. 서킷브레이커가 활성화되어 있어 호출 자체가 거부 -> commerce에 요청이 가지도 않은 상태
     *
     * 차단기의 효과는 실패하던 것을 즉시 실패로 바꾸어 다른 요청의 스레드를 지키는 데 있다.
     */
    private Void createWalletFallback(final Throwable cause) {
        log.warn("지갑 생성 호출 실패 -> fallback 실행. 원인: {}", cause.toString());

        throw new ServiceUnavailableException(MemberErrorCode.WALLET_CREATION_FAILED);
    }
}
