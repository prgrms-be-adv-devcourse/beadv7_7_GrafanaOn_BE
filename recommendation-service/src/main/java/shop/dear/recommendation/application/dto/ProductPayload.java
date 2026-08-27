package shop.dear.recommendation.application.dto;

import org.springframework.util.StringUtils;

//product 이벤트에서 보내줄 payload
public record ProductPayload(Long productId, String story) {

	public boolean hasProductId() {
		return this.productId != null;
	}

	public boolean hasStory() {
		return StringUtils.hasText(this.story);
	}
}
