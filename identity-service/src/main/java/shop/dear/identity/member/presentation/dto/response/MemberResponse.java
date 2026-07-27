package shop.dear.identity.member.presentation.dto.response;

import shop.dear.identity.member.application.dto.MemberInfo;

public record MemberResponse(
    Long id,
    String name,
    String defaultShippingAddress,
    String phoneNumber,
    String nickname
) {

    public static MemberResponse from(MemberInfo info, boolean isOwner) {

        String address = isOwner ? info.defaultShippingAddress() : maskAddress(info.defaultShippingAddress());
        String phoneNumber = isOwner ? info.phoneNumber() : maskPhoneNumber(info.phoneNumber());

        return new MemberResponse(
            info.id(),
            info.name(),
            address,
            phoneNumber,
            info.nickname()
        );
    }

    private static String maskAddress(final String address) {

        if (address == null || address.isBlank()) {
            return address;
        }

        return address.substring(0, 3);
    }

    private static String maskPhoneNumber(final String phoneNumber) {

        if (phoneNumber == null || phoneNumber.isBlank()) {
            return phoneNumber;
        }

        String[] parts = phoneNumber.split("-");

        return parts[0] + "-" + "*".repeat(3) + "-" + parts[2];
    }
}
