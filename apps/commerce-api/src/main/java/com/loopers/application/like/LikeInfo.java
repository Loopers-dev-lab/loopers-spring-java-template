package com.loopers.application.like;

public record LikeInfo(long likeCount, boolean isLiked) {
  public static LikeInfo from(long likeCount, boolean isLiked) {
    return new LikeInfo(
        likeCount,
        isLiked
    );
  }
}
