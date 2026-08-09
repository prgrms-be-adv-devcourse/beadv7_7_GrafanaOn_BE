package shop.dear.identity.member.application;

public interface Encryptor {

    String encode(String raw);
    String decode(String encoded);
}
