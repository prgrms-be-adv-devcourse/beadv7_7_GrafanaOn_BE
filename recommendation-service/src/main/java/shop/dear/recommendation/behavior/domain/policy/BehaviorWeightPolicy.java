package shop.dear.recommendation.behavior.domain.policy;

import org.springframework.stereotype.Component;
import shop.dear.recommendation.behavior.domain.constant.BehaviorType;

@Component
public class BehaviorWeightPolicy {

    public double getWeight(final BehaviorType behaviorType) {
        return switch (behaviorType) {
            case VIEW -> 1.0;
            case CLICK -> 2.0;
            case SCRAP -> 4.0;
            case CART_ADD -> 5.0;
            case PURCHASE -> 7.0;
            case IMPRESSION -> 0.0;
        };
    }

    public boolean contributesToInterest(final BehaviorType behaviorType) {
        return behaviorType != BehaviorType.IMPRESSION;
    }
}