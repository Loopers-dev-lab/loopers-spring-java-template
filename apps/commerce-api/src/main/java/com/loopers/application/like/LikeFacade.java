package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductLikeCountService;
import com.loopers.domain.product.ProductService;
import com.loopers.messaging.event.LikeCanceledEvent;
import com.loopers.messaging.event.LikeCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LikeFacade {
    private final LikeService likeService;
    private final ProductService productService; // ProductService 주입
    private final ProductLikeCountService productLikeCountService; // ProductLikeCountService 주입

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void addLike(Long userId, Long productId) {
        // 1. 상품 검증
        Product product = productService.getProduct(productId);

        // 2. 좋아요 생성 시도
        boolean isNew = likeService.createLike(Like.create(userId, productId));

        // 3. 신규 좋아요일 때만 이벤트를 통한 후속 처리 (카운트 증가)
        if (isNew) {
            eventPublisher.publishEvent(LikeCreatedEvent.from(productId));
        }
    }

    @Transactional
    public void removeLike(Long userId, Long productId) {
        // 1. 상품 검증
        Product product = productService.getProduct(productId);

        // 2. 좋아요 취소 시도
        boolean isDeleted = likeService.deleteLike(Like.create(userId, productId));

        // 3. 좋아요가 실제로 취소된 경우만 이벤트를 통한 후속 처리 (카운트 차감)
        if(isDeleted) {
            eventPublisher.publishEvent(LikeCanceledEvent.from(productId));
        }
    }

    @Transactional(readOnly = true)
    public List<LikeInfo> getLikedProducts(Long userId) {
        List<Like> likes = likeService.findAll(userId);

        List<Long> productIds = likes.stream()
                .map(Like::getProductId)
                .toList();

        if (productIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> productsById = productService.getProductsMapByIds(productIds);

        return productIds.stream()
                .map(productId -> {
                    Product product = productsById.get(productId);

                    // 좋아요 수를 ProductLikeCountService에서 조회 (없으면 0)
                    int likeCount = productLikeCountService.findById(productId)
                            .map(pc -> pc.getLikeCount())
                            .orElse(0);

                    return new LikeInfo(
                            userId,
                            productId,
                            likeCount
                    );
                })
                .collect(Collectors.toList());
    }
}
