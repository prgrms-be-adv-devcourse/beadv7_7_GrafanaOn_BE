package shop.dear.recommendation.behavior.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.dear.audit.BaseEntity;
import shop.dear.commerce.product.domain.constant.ProductCategory;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_interest",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_interest_member_category",
                        columnNames = {"member_id", "category"}
                )
        }
)
public class UserInterest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ProductCategory category;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "last_calculated_at", nullable = false)
    private LocalDateTime lastCalculatedAt;

    private UserInterest(
            final Long memberId,
            final ProductCategory category,
            final Double score,
            final LocalDateTime lastCalculatedAt
    ) {
        this.memberId = memberId;
        this.category = category;
        this.score = score;
        this.lastCalculatedAt = lastCalculatedAt;
    }

    public static UserInterest create(
            final Long memberId,
            final ProductCategory category,
            final Double score,
            final LocalDateTime lastCalculatedAt
    ){
        return new UserInterest(memberId, category, score, lastCalculatedAt);
    }

    public void update(
            final Double score,
            final LocalDateTime lastCalculatedAt
    ) {
        this.score = score;
        this.lastCalculatedAt = lastCalculatedAt;
    }

}
