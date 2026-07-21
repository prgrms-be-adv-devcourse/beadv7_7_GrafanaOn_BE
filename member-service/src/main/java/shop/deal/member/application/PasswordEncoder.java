package shop.deal.member.application;

public interface PasswordEncoder {

    String encode(String rawPassword);
}
