package com.loopers.domain.like;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor

@Service
public class LikeService {

    private final LikeRepository likeRepository;

    public void recordLike(Long userId, Long productId) {
        if (likeRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }
        likeRepository.saveLike(Like.createLike(userId, productId));
    }

    public void cancelLike(Long userId, Long productId) {
        if (!likeRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }
        likeRepository.delete(userId, productId);
    }

    public boolean recordLikeIfAbsent(Long userId, Long productId) {
        return likeRepository.saveIfAbsent(userId, productId);
    }

    public boolean cancelLikeIfPresent(Long userId, Long productId) {
        return likeRepository.deleteIfPresent(userId, productId);
    }

    public List<Long> findLikedProductIds(Long userId) {
        return likeRepository.findProductIdsByUserId(userId);
    }

}
