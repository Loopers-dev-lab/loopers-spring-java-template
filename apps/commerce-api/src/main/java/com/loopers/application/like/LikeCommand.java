package com.loopers.application.like;

public class LikeCommand {
    public record Like (
        String userId,
        Long productId
    ) { }
}
