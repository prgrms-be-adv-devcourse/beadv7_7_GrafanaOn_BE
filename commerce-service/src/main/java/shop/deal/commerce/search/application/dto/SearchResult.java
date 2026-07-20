package shop.deal.commerce.search.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
