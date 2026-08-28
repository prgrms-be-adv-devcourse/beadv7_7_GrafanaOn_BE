-- ===========================================================================
-- product_vector 스키마 (운영)
--
-- 왜 필요한가:
--   운영은 recommendation.vector.initialize-schema=false 다.
--   애플리케이션에 DDL 권한을 주지 않기 때문에 ProductVectorSchemaInitializer 빈이
--   만들어지지 않는다. 배포 전에 이 파일을 한 번 실행해 테이블을 만들어 둔다.
--   (rec_recommendation.md 5.1 / 6.3)
--
-- 차원:
--   VECTOR(1536) — OpenAI text-embedding-3-small 의 네이티브 차원.
--   로컬은 bge-m3 라 1024 다. 모델이 다르면 벡터 공간도 달라 어차피 공유할 수 없으므로
--   차원을 통일하지 않는다. 이 파일은 운영 전용이다.
--
-- Index:
--   일부러 만들지 않는다. 상품 수가 적을 때는 Sequential Scan 이 더 빠를 수 있어,
--   실제 데이터로 EXPLAIN ANALYZE 를 떠 본 뒤 HNSW / IVFFlat 도입을 판단한다.
--   (rec_recommendation.md 9.1)
--
-- 실행:
--   psql -h <host> -U <user> -d <db> -f sql/schema-product-vector.sql
-- ===========================================================================

CREATE EXTENSION IF NOT EXISTS vector;

CREATE SCHEMA IF NOT EXISTS recommendation;

CREATE TABLE IF NOT EXISTS recommendation.product_vector (
	product_id  BIGINT       PRIMARY KEY,           -- 상품당 벡터 1개
	story       TEXT         NOT NULL,              -- 임베딩에 넣은 원문. 재임베딩 판단 기준이다.
	model_name  VARCHAR(100) NOT NULL,              -- 모델이 다르면 벡터 공간도 다르다.
	embedding   VECTOR(1536) NOT NULL,              -- Local 1024 / Production 1536
	embedded_at TIMESTAMP    NOT NULL
);
