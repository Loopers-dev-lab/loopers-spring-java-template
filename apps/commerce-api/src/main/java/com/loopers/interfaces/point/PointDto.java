package com.loopers.interfaces.point;

import com.loopers.application.point.PointResult;

public class PointDto {

    public record Response(
        String userId,
        Long balance
    ) {
        public static Response from(PointResult pointResult) {
            return new Response(
                pointResult.userId(),
                pointResult.balance()
            );
        }
    }
}
