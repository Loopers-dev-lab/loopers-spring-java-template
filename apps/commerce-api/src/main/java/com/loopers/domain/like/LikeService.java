package com.loopers.domain.like;

import com.loopers.domain.like.event.LikeEvent;
import com.loopers.domain.like.event.LikeEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * packageName : com.loopers.application.like
 * fileName     : LikeService
 * author      : byeonsungmun
 * date        : 2025. 11. 12.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 12.     byeonsungmun       최초 생성
 */
@Component
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final LikeEventPublisher likeEventPublisher;

    @Transactional
    public void like(String userId, Long productId) {
        if (likeRepository.findByUserIdAndProductId(userId, productId).isPresent()) return;

        Like like = Like.create(userId, productId);
        likeRepository.save(like);
        likeEventPublisher.publish(LikeEvent.ProductLiked.of(userId, productId));
    }

    @Transactional
    public void unlike(String userId, Long productId) {
        likeRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(like -> {
                    likeRepository.delete(like);
                    likeEventPublisher.publish(LikeEvent.ProductUnliked.of(userId, productId));
                });
    }

    @Transactional(readOnly = true)
    public long countByProductId(Long productId) {
        return likeRepository.countByProductId(productId);
    }

}
