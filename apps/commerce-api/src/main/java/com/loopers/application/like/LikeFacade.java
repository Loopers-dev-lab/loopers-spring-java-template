package com.loopers.application.like;

import com.loopers.application.product.ProductCacheService;
import com.loopers.domain.like.LikeEvent;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeFacade {

    private final LikeService likeService;
    private final UserService userService;
    private final ProductService productService;
    private final ProductCacheService productCacheService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void addLike(String loginId, Long productId) {
        User user = userService.getUserByLoginId(loginId);
        Product product = productService.getProductWithPessimisticLock(productId);

        boolean isNewLike = likeService.addLike(user, product);

        if (isNewLike) {
            eventPublisher.publishEvent(LikeEvent.added(user.getId(), productId));
            log.info("좋아요 등록 이벤트 발행: userId={}, productId={}", user.getId(), productId);
        }
    }

    @Transactional
    public void removeLike(String loginId, Long productId) {
        User user = userService.getUserByLoginId(loginId);
        Product product = productService.getProductWithPessimisticLock(productId);

        boolean wasRemoved = likeService.removeLike(user, product);

        if (wasRemoved) {
            eventPublisher.publishEvent(LikeEvent.removed(user.getId(), productId));
            log.info("좋아요 취소 이벤트 발행: userId={}, productId={}", user.getId(), productId);
        }
    }

    @Transactional
    public void incrementLikeCount(Long productId) {
        productService.incrementLikeCount(productId);
    }

    @Transactional
    public void decrementLikeCount(Long productId) {
        productService.decrementLikeCount(productId);
    }

    public void evictLikeRelatedCache(Long productId) {
        productCacheService.evictProductDetailCache(productId);
        productCacheService.evictProductListCachesByLikesSort();
    }
}
