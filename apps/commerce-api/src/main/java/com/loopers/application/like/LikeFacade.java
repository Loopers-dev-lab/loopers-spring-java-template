package com.loopers.application.like;

import com.loopers.domain.like.LikeService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class LikeFacade {

    private final LikeService likeService;
    private final UserService userService;

    /**
     * 상품 좋아요 등록
     */
    @Transactional
    public void saveProductLike(String loginId, Long productId) {
        User user = userService.findUserByLoginId(loginId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "[loginId = " + loginId + "] User를 찾을 수 없습니다."
                ));

        likeService.saveProductLike(user, productId);
    }

    /**
     * 상품 좋아요 취소
     */
    @Transactional
    public void deleteProductLike(String loginId, Long productId) {
        User user = userService.findUserByLoginId(loginId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "[loginId = " + loginId + "] User를 찾을 수 없습니다."
                ));

        likeService.deleteProductLike(user, productId);
    }
}

