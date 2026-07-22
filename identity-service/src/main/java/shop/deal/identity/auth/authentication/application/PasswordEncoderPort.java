package shop.deal.identity.auth.authentication.application;

// Auth 서비스는 다른 암호화 알고리즘을 알지 못하고 이 인터페이스만 사용한다.
public interface PasswordEncoderPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
