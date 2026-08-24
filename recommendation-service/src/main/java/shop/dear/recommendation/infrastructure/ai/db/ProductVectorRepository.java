package shop.dear.recommendation.infrastructure.ai.db;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.pgvector.PGvector;

// 상품 Story 벡터 저장소.

@Repository
public class ProductVectorRepository {

	private final JdbcTemplate jdbcTemplate;

	public ProductVectorRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void upsert(Long productId, String story, String modelName, float[] embedding) {
		this.jdbcTemplate.update("""
			INSERT INTO product_vector (product_id, story, model_name, embedding, embedded_at)
			VALUES (?, ?, ?, ?, now())
			ON CONFLICT (product_id) DO UPDATE
			   SET story       = EXCLUDED.story,
			       model_name  = EXCLUDED.model_name,
			       embedding   = EXCLUDED.embedding,
			       embedded_at = EXCLUDED.embedded_at
			""", productId, story, modelName, new PGvector(embedding));
	}

	// 이 상품에 대한 Story 원문
	public Optional<String> findStory(Long productId, String modelName) {
		List<String> found = this.jdbcTemplate.queryForList(
			"SELECT story FROM product_vector WHERE product_id = ? AND model_name = ?",
			String.class, productId, modelName
		);
		return found.stream().findFirst();
	}
}
