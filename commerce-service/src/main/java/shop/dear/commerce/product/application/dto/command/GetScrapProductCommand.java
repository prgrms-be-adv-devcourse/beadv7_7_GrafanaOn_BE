package shop.dear.commerce.product.application.dto.command;

import java.util.List;

public record GetScrapProductCommand(
    List<Long> ids
) {
}
