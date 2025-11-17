package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class LikeRepositoryImpl implements LikeRepository {

    private final LikeJpaRepository likeJpaRepository;

    @Override
    public void saveLike(Like like) {
        try {
            likeJpaRepository.save(like);
        } catch (DataIntegrityViolationException e) {
            throw new CoreException(ErrorType.CONFLICT, "이미 좋아요가 등록된 상품입니다.");
        }
    }

    @Override
    public void delete(Long userId, Long productId) {
        likeJpaRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Override
    public boolean saveIfAbsent(Long userId, Long productId) {
        try {
            Like like = Like.createLike(userId, productId);
            likeJpaRepository.save(like);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Override
    public boolean deleteIfPresent(Long userId, Long productId) {
        int deletedCount = likeJpaRepository.deleteByUserIdAndProductIdAndReturnCount(userId, productId);
        return deletedCount > 0;
    }

    @Override
    public List<Long> findProductIdsByUserId(Long userId) {
        return likeJpaRepository.findProductIdsByUserId(userId);
    }

    @Override
    public boolean existsByUserIdAndProductId(Long userId, Long productId) {
        return likeJpaRepository.existsByUserIdAndProductId(userId, productId);
    }
}

