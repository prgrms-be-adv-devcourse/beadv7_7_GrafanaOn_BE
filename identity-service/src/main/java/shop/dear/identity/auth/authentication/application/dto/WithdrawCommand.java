package shop.dear.identity.auth.authentication.application.dto;

// 추후 reason같은 변수가 추가될 수 있음
public record WithdrawCommand(
        Long memberId
) {
}
