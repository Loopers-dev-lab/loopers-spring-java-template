package com.loopers.application.like;

import com.loopers.domain.like.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
public class LikeFacade {

    private final LikeService likeService;

    public void createLike(String userId, Long productId) {
        try {
            likeService.like(userId, productId);
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException e) {
            return;
        }
    }

    public void deleteLike(String userId, Long productId) {
        try {
            likeService.unlike(userId, productId);
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException e) {
            return;
        }
    }
}

