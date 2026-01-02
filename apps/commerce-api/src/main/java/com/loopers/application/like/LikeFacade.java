package com.loopers.application.like;

import com.loopers.domain.like.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * packageName : com.loopers.application.like
 * fileName     : LikeFacade
 * author      : byeonsungmun
 * date        : 2025. 11. 14.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 14.     byeonsungmun       최초 생성
 */
@Component
@RequiredArgsConstructor
public class LikeFacade {

    private final LikeService likeService;

    public void like(String userId, Long productId) {
        likeService.like(userId, productId);
    }

    public void unlike(String userId, Long productId) {
        likeService.unlike(userId, productId);
    }
}
