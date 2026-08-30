package shop.dear.recommendation.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import shop.dear.common.exception.BusinessException;
import shop.dear.recommendation.domain.exception.RecommendationErrorCode;
import shop.dear.recommendation.domain.model.RecommendationSimilarItem;
import shop.dear.recommendation.domain.repository.ProductVectorRepository;

import java.util.List;

@Slf4j
@Service
public class RecommendationService {

	private final ProductVectorRepository productVectorRepository;

	private final double maxDistance;
	private final int defaultSize;
	private final int maxSize;

	public RecommendationService(
		final ProductVectorRepository productVectorRepository,
		@Value("${recommendation.similar.max-distance}") final double maxDistance,
		@Value("${recommendation.similar.default-size}") final int defaultSize,
		@Value("${recommendation.similar.max-size}") final int maxSize
	) {
		this.productVectorRepository = productVectorRepository;
		this.maxDistance = maxDistance;
		this.defaultSize = defaultSize;
		this.maxSize = maxSize;
	}


	// 기준 상품의 벡터와 나머지 상품 벡터 사이의 Cosine Distance 를 재서 가까운 순으로 돌려준다.
	public List<RecommendationSimilarItem> findSimilarItems(final Long productId, final Integer size) {

		if (productId == null) {
			throw new BusinessException(RecommendationErrorCode.PRODUCT_ID_REQUIRED);
		}

		final int limit = resolveSize(size);

		final List<RecommendationSimilarItem> items = this.productVectorRepository.findSimilar(
			productId, this.maxDistance, limit
		);

		log.info("상품 {} 유사 상품 {}건 (limit={}, maxDistance={})",
			productId, items.size(), limit, this.maxDistance);

		return items;
	}

	private int resolveSize(final Integer size) {
		if (size == null || size < 1) {
			return this.defaultSize;
		}
		return Math.min(size, this.maxSize);
	}
}
