package shop.dear.identity.member.application.dto;

public record UpdateSellerAccountCommand(
    String bank,
    String account
){
}
