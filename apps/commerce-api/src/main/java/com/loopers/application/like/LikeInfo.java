package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeTargetType;
import lombok.Builder;

@Builder
public record LikeInfo(
        Long id
        , Long userId
        , Long likeTargetId
        , LikeTargetType likeTargetType
) {
    public static LikeInfo from(Like like) {
        return LikeInfo.builder()
                .id(like.getId())
                .userId(like.getUser().getId())
                .likeTargetId(like.getLikeTargetId())
                .likeTargetType(like.getLikeTargetType())
                .build();
    }
}

