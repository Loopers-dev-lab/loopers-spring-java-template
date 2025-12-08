package com.loopers.application.like;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.like.LikeResult;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.ProductCacheService;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 좋아요 애플리케이션 파사드
 * <p>
 * 좋아요 관련 유스케이스를 조정합니다.
 * 여러 도메인 서비스(User, Product, Like)를 조합하여 완전한 비즈니스 흐름을 구현합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeFacade {
    private final ProductService productService;
    private final UserService userService;
    private final LikeService likeService;
    private final ProductCacheService cacheService;

    /**
     * 좋아요를 등록하거나 복원합니다.
     *
     * @param username  사용자명
     * @param productId 상품 ID
     * @return 좋아요 정보 DTO
     * @throws CoreException 사용자 또는 상품을 찾을 수 없는 경우
     */
    @Transactional
    public LikeInfo upsertLike(String username, Long productId) {
        log.debug("좋아요 등록/복원 시작 - username: {}, productId: {}", username, productId);

        // 1. 사용자 검증
        UserEntity user = userService.getUserByUsername(username);

        // 2. 상품 검증
        ProductEntity product = productService.getActiveProductDetail(productId);

        // 3. 좋아요 등록/복원 (실제 변경 여부 확인)
        LikeResult result = likeService.upsertLike(user, product);

        // 5. DTO 변환 후 반환
        return LikeInfo.of(result.entity(), product, user);
    }

    /**
     * 좋아요를 취소합니다.
     * <p>
     * 좋아요를 삭제하고 상품의 좋아요 카운트를 감소시킵니다.
     *
     * @param username  사용자명
     * @param productId 상품 ID
     * @throws CoreException 사용자 또는 상품을 찾을 수 없는 경우
     */
    @Transactional
    public void unlikeProduct(String username, Long productId) {
        log.debug("좋아요 취소 시작 - username: {}, productId: {}", username, productId);

        // 1. 사용자 검증
        UserEntity user = userService.getUserByUsername(username);

        // 2. 상품 검증
        ProductEntity product = productService.getActiveProductDetail(productId);

        // 3. 좋아요 취소
        likeService.unlikeProduct(user, product);
    }
}
