package shop.deal.commerce.search.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Search가 소유하는 도메인 모델.
@Getter
@AllArgsConstructor
public class SearchProduct {

    private final Long productId;
    private final String productName;
    private final String modelNumber;
    private final String category;
    private final LocalDate releaseDate;
    private final BigDecimal productPrice;
    private final String saleType;
    private final Long viewCount;
    private final String description;
    private final LocalDateTime insertedAt;
}
