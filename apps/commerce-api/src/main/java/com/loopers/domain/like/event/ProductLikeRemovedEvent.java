package com.loopers.domain.like.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProductLikeRemovedEvent {
    private final Long likeId;
    private final Long productId;
}
