package shop.dear.identity.member.domain.model;

import java.time.LocalDateTime;

public record SellerAccountSnapshot(
    Long memberId,
    AccountInfo accountInfo,
    LocalDateTime withdrawnAt
) {
}
