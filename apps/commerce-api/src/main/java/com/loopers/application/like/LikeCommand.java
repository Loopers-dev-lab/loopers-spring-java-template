package com.loopers.application.like;

public class LikeCommand {

    public record LikeProductCommand(
            String loginId,
            Long productId
    ) {
    }
}

