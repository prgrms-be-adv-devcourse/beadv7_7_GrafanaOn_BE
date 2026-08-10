package shop.dear.identity.member.infrastructure.persistence;

public interface CipherStrategy {

	boolean supports(String encoded);
	String decode(String encoded);
}
