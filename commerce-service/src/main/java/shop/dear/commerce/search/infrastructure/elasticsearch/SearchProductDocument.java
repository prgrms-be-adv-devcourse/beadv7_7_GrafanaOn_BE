package shop.dear.commerce.search.infrastructure.elasticsearch;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import shop.dear.commerce.search.domain.SearchProduct;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Elasticsearch 검색 문서입니다.
 * 문서 ID는 Product가 발급한 id를 그대로 사용합니다.
 * 같은 상품을 다시 색인해도 덮어쓰기가 되어 중복이 생기지 않습니다.
 * 상품명, 설명, 스토리 검색은 nori를 사용합니다.
 * 카테고리, 판매유형, 모델번호는 keyword에 정확히 일치하는 검색을 진행합니다. 분석기를 사용하지 않습니다.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Document(indexName = "search_product")
@Setting(shards = 1, replicas = 0) // 단일 노드 구성
public class SearchProductDocument {

    @Id // Elasticsearch는 springframework의 ID를 인식함. jakarta.persistence 사용 X
    private Long productId;

    // nori 사용
    @Field(name = "name", type = FieldType.Text, analyzer = "nori")
    private String productName;

    @Field(name = "model_number", type = FieldType.Keyword)
    private String modelNumber;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(name = "release_date", type = FieldType.Date, format = DateFormat.date)
    private LocalDate releaseDate;

    @Field(name = "price", type = FieldType.Double)
    private BigDecimal productPrice;

    @Field(name = "sale_type", type = FieldType.Keyword)
    private String saleType;

    @Field(name = "view_count", type = FieldType.Long)
    private Long viewCount;

    // nori 사용
    @Field(type = FieldType.Text, analyzer = "nori")
    private String description;

    // nori 사용
    @Field(name = "story_content", type = FieldType.Text, analyzer = "nori")
    private String storyContent;

    @Field(name = "product_inserted_at", type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime productInsertedAt;

    public static SearchProductDocument from(final SearchProduct product) {
        return SearchProductDocument.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .modelNumber(product.getModelNumber())
                .category(product.getCategory())
                .releaseDate(product.getReleaseDate())
                .productPrice(product.getProductPrice())
                .saleType(product.getSaleType())
                .viewCount(product.getViewCount())
                .description(product.getDescription())
                .storyContent(product.getStoryContent())
                .productInsertedAt(product.getProductInsertedAt())
                .build();
    }

    public SearchProduct toDomain() {
        return new SearchProduct(
                productId,
                productName,
                modelNumber,
                category,
                releaseDate,
                productPrice,
                saleType,
                viewCount,
                description,
                storyContent,
                productInsertedAt
        );
    }
}