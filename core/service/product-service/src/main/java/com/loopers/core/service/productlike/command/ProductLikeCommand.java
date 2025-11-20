package com.loopers.core.service.productlike.command;

import lombok.Getter;

@Getter
public class ProductLikeCommand {

    private final String userIdentifier;
    private final String ProductId;

    public ProductLikeCommand(String userIdentifier, String productId) {
        this.userIdentifier = userIdentifier;
        ProductId = productId;
    }
}
