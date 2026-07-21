package shop.deal.member.domain;

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

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String defaultShippingAddress;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
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
                nickname);
    }
}
