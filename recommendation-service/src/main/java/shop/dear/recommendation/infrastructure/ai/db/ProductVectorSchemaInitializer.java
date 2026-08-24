package shop.dear.recommendation.infrastructure.ai.db;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

// 기동 시 product_vector 테이블 생성
@Slf4j
@Component
@ConditionalOnProperty(name = "recommendation.vector.initialize-schema", havingValue = "true")
public class ProductVectorSchemaInitializer {

	private final JdbcTemplate jdbcTemplate;
	private final int dimensions;

	public ProductVectorSchemaInitializer(
		JdbcTemplate jdbcTemplate,
		@Value("${recommendation.vector.dimensions}") int dimensions
	) {
		this.jdbcTemplate = jdbcTemplate;
		this.dimensions = dimensions;
	}

	@PostConstruct
	public void initialize() {
		this.jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
		this.jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS product_vector (
			    product_id  BIGINT       PRIMARY KEY,
			    story       TEXT         NOT NULL,
			    model_name  VARCHAR(100) NOT NULL,
			    embedding   VECTOR(%d)   NOT NULL,
			    embedded_at TIMESTAMP    NOT NULL
			)
			""".formatted(this.dimensions));

		log.info("product_vector 준비 완료 (dimensions={})", this.dimensions);
	}
}
