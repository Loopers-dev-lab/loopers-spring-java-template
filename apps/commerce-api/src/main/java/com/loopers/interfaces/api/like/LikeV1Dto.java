package com.loopers.interfaces.api.like;

import com.loopers.interfaces.api.order.OrderV1Dto;

public class LikeV1Dto {

    public record LikeRequest(
            String userId,
            Long productId
    ) {

    }

    public record LikeResponse(
            String userId,
            Long productId
    ) {

    }
}
