package shop.dear.identity.member.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.common.audit.BaseEntity;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 30, nullable = false)
    private String name;

    @Column(name = "default_shipping_address",nullable = false)
    private String defaultShippingAddress;

    @Column(name = "phone_number", length = 20, nullable = false)
    private String phoneNumber;

    @Column(name = "nickname", nullable = false, unique = true)
    private String nickname;

    private Member(
        final String name,
        final String defaultShippingAddress,
        final String phoneNumber,
        final String nickname
    ) {
        this.name = name;
        this.defaultShippingAddress = defaultShippingAddress;
        this.phoneNumber = phoneNumber;
        this.nickname = nickname;
    }

    public static Member create(
        final String name,
        final String defaultShippingAddress,
        final String phoneNumber,
        final String nickname
    ) {
        return new Member(
            name,
            defaultShippingAddress,
            phoneNumber,
            nickname
        );
    }

    public void changeProfile(
        final String defaultShippingAddress,
        final String phoneNumber,
        final String nickname
    ) {
        this.defaultShippingAddress = defaultShippingAddress;
        this.phoneNumber = phoneNumber;
        this.nickname = nickname;
    }
}
