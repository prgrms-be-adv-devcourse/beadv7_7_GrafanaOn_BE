package shop.dear.recommendation.infrastructure.persistence;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// Spring AI에서 제공하는 스키마를 사용하지 않고 별도 product_vector 테이블정의
@Slf4j
@Component
@ConditionalOnProperty(prefix = "recommendation.vector", name = "initialize-schema", havingValue = "true")
public class ProductVectorSchemaInitializer {

	private final JdbcTemplate jdbcTemplate;
	private final String schema;
	private final int dimensions;

	public ProductVectorSchemaInitializer(
		JdbcTemplate jdbcTemplate,
		@Value("${recommendation.vector.schema}") String schema,
		@Value("${recommendation.vector.dimensions}") int dimensions
	) {
		this.jdbcTemplate = jdbcTemplate;
		this.schema = schema;
		this.dimensions = dimensions;
	}

	@PostConstruct
	public void initialize() {
		this.jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
		this.jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS %s.product_vector (
			    product_id  BIGINT       PRIMARY KEY,
			    story       TEXT         NOT NULL,
			    model_name  VARCHAR(100) NOT NULL,
			    embedding   VECTOR(%d)   NOT NULL,
			    embedded_at TIMESTAMP(6)    NOT NULL
			)
			""".formatted(this.schema, this.dimensions));
	}
}
