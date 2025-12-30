package com.loopers.application.like;

import com.loopers.domain.like.entity.Like;
import com.loopers.domain.like.entity.LikeTargetType;

import lombok.Builder;

@Builder
public record LikeInfo(
        Long userId
        , Long likeTargetId
        , LikeTargetType likeTargetType
) {
    public static LikeInfo from(Like like) {
        return LikeInfo.builder()
                .userId(like.getLikeId().getUserId())
                .likeTargetId(like.getLikeId().getLikeTargetId())
                .likeTargetType(like.getLikeId().getLikeTargetType())
                .build();
    }
}

