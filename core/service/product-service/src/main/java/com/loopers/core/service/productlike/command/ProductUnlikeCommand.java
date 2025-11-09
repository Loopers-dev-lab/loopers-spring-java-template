package com.loopers.core.service.productlike.command;

import lombok.Getter;

@Getter
public class ProductUnlikeCommand {

    private final String userIdentifier;

    private final String productId;

    public ProductUnlikeCommand(String userIdentifier, String productId) {
        this.userIdentifier = userIdentifier;
        this.productId = productId;
    }
}
