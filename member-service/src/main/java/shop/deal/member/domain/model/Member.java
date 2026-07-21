package shop.deal.member.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.deal.common.audit.BaseEntity;

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

    @Column(name = "email", length = 150, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "default_shipping_address",nullable = false)
    private String defaultShippingAddress;

    @Column(name = "phone_number", length = 20, nullable = false)
    private String phoneNumber;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    private Member(
        final String name,
        final String email,
        final String password,
        final String defaultShippingAddress,
        final String phoneNumber,
        final String nickname
    ) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.defaultShippingAddress = defaultShippingAddress;
        this.phoneNumber = phoneNumber;
        this.nickname = nickname;
    }

    public static Member create(
        final String name,
        final String email,
        final String password,
        final String defaultShippingAddress,
        final String phoneNumber,
        final String nickname
    ) {
        return new Member(
            name,
            email,
            password,
            defaultShippingAddress,
            phoneNumber,
            nickname
        );
    }
}
