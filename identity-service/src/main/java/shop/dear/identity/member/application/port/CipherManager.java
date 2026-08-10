package shop.dear.identity.member.application.port;

public interface CipherManager {

    String encode(String raw);
    String decode(String encoded);
}
