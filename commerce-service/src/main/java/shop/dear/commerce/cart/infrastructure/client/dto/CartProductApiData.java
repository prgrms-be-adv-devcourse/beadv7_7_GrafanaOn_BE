package shop.dear.commerce.cart.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CartProductApiData(
        Long id,
        String name,
        BigDecimal price,
        List<ImageInfo> images,
        String status
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImageInfo(
            String url
    ) {
    }
}
