package shop.dear.identity.member.presentation.dto.response;

public record MemberCheckResponse(
    boolean exists
) {

    public static MemberCheckResponse from(boolean exists) {
        return new MemberCheckResponse(exists);
    }
}
