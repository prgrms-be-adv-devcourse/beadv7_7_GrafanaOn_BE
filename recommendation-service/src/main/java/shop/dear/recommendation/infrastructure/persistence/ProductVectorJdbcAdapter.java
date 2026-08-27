package shop.dear.recommendation.infrastructure.persistence;

import com.pgvector.PGvector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import shop.dear.recommendation.domain.model.Embedding;
import shop.dear.recommendation.domain.model.RecommendationItem;
import shop.dear.recommendation.domain.repository.ProductVectorRepository;

import java.util.List;
import java.util.Optional;

//Hibernate 에 vector 타입이 없어 JPA 가 아니라 JdbcTemplate을 사용
@Repository
public class ProductVectorJdbcAdapter implements ProductVectorRepository {

	private final JdbcTemplate jdbcTemplate;
	private final String table;

	public ProductVectorJdbcAdapter(
		final JdbcTemplate jdbcTemplate,
		@Value("${recommendation.vector.schema}") final String schema
	) {
		this.jdbcTemplate = jdbcTemplate;
		this.table = schema + ".product_vector";
	}

	@Override
	public void save(final Long productId, final String story, final Embedding embedding) {
		this.jdbcTemplate.update("""
			INSERT INTO %s (product_id, story, model_name, embedding, embedded_at)
			VALUES (?, ?, ?, ?, now())
			ON CONFLICT (product_id) DO UPDATE
			   SET story       = EXCLUDED.story,
			       model_name  = EXCLUDED.model_name,
			       embedding   = EXCLUDED.embedding,
			       embedded_at = EXCLUDED.embedded_at
			""".formatted(this.table),
			productId,
			story,
			embedding.modelName(),
			new PGvector(embedding.values())
		);
	}

	@Override
	public Optional<String> findStory(final Long productId) {
		return this.jdbcTemplate.queryForList(
			"SELECT story FROM %s WHERE product_id = ?".formatted(this.table),
			String.class, productId
		).stream().findFirst();
	}

	@Override
	public int deleteByProductId(final Long productId) {
		return this.jdbcTemplate.update(
			"DELETE FROM %s WHERE product_id = ?".formatted(this.table), productId
		);
	}

	@Override
	public List<RecommendationItem> findSimilar(
		final Long productId, final double maxDistance, final int limit
	) {
		return this.jdbcTemplate.query("""
			SELECT s.product_id                AS product_id,
			       s.embedding <=> b.embedding AS distance
			  FROM %1$s s,
			       %1$s b
			 WHERE b.product_id  = ?
			   AND s.product_id <> b.product_id
			   AND (s.embedding <=> b.embedding) < ?
			 ORDER BY distance
			 LIMIT ?
			""".formatted(this.table),
			(rs, rowNum) -> new RecommendationItem(rs.getLong("product_id"), rs.getDouble("distance")),
			productId, maxDistance, limit
		);
	}
}
