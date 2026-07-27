package shop.dear.commerce.product.presentation.dto.request;

import shop.dear.commerce.product.application.dto.command.GetScrapProductCommand;

import java.util.List;

public record GetScrapProductsRequest(
    List<Long> ids
) {

    public GetScrapProductCommand toCommand() {
        return new GetScrapProductCommand(ids.stream().toList());
    }
}
