package shop.dear.identity.auth.authentication.infrastructure.security;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 인증은 게이트웨이가 이미 끝냈다. 여기서는 전부 열어둔다.
 * oauth2-client 의존성이 들어오면 Spring Security 기본 설정이 켜져
 * 모든 요청이 인증을 요구하게 되기 때문이다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class IdentitySecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain identityFilterChain(final HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())

                // 게이트웨이가 인증을 끝내고 X-Authenticated-Member-Id를 붙여 보낸다.
                // identity가 또 다시 판단하지 않는다는 것이다.
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        // 기본 경로는 현재 게이트웨이가 라우팅 할 수 없다. /api 가 붙지 않았기 때문이다.
                        // 따라서 /apu/auth 아래로 옮긴다.
                        .authorizationEndpoint(endpoint -> endpoint.baseUri("/api/auth/oauth2/authorization"))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/api/auth/oauth2/callback/*"))
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .build();
    }
}
