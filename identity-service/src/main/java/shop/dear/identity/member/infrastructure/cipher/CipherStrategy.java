package shop.dear.identity.member.infrastructure.cipher;

public interface CipherStrategy {

	boolean supports(String encoded);
	String decode(String encoded);
}
