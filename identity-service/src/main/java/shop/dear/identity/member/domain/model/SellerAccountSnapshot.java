package shop.dear.identity.member.domain.model;

import java.time.LocalDateTime;

/**
 * 보관 이관 시점에 셀러에서 떼어낸 계좌정보.
 * 계좌정보를 지우기 전 값을 고정해두기 위한 것으로,
 * Member 애그리거트와 ArchivedAccount 애그리거트가 서로를 몰라도 되게 한다.
 */
public record SellerAccountSnapshot(
    Long memberId,
    AccountInfo accountInfo,
    LocalDateTime withdrawnAt
) {
}
