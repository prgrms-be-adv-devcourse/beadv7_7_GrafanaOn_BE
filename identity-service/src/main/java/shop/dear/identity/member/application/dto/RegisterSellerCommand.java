package shop.dear.identity.member.application.dto;

public record RegisterSellerCommand(
    String bank,
    String account
){
}
