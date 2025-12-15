package com.loopers.domain.like;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class LikeService {

    private final LikeRepository likeRepository;

    public Optional<Like> findLike(Long userId, Long productId) {
        return likeRepository.findByUserIdAndProductId(userId, productId);
    }

    public boolean createLike(Like like) {
        return likeRepository.save(like);
    }

    public boolean deleteLike(Like like) {
        return likeRepository.delete(like);
    }

    public List<Like> findAll(Long userId) {
        return likeRepository.findAllByUserId(userId);
    }
}
