package com.loopers.interfaces.api.like;

import java.util.List;

public class LikeV1Dto {
    public record LikedProductsResponse(List<Long> productIds) {}
}
