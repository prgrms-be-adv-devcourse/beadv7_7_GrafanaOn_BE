package shop.dear.identity.scrap.infrastructure.client.dto;

import java.util.List;

public record GetProductsBody(
    List<Long> ids
) {
}
