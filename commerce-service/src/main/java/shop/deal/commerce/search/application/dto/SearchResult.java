package shop.deal.commerce.search.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SearchResult (
        String productName,
        String modelNumber,
        String category,
        LocalDate releaseDate,
        BigDecimal productPrice,
        String saleType,
        Long viewCount,
        String description
){
}