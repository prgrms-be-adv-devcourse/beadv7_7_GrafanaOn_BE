package shop.deal.identity.auth.authentication.application.dto;

// Member로부터 받을 결과 DTO
public record MemberProfileResult(
        Long memberId,
        String nickname
) {
}
