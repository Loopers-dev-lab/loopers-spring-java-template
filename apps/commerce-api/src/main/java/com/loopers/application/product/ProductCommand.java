package com.loopers.application.product;

public class ProductCommand {

    public record GetProductsCommand(
            Long brandId,
            ProductSort sort,
            int page,
            int size
    ) {
    }
}

