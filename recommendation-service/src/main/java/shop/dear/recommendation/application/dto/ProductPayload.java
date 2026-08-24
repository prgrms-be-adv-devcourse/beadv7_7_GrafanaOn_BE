package shop.dear.recommendation.application.dto;

import org.springframework.util.StringUtils;

public record ProductPayload(Long productId, String story) {

	public boolean isValid() {
		return this.productId != null && StringUtils.hasText(this.story);
	}
}
