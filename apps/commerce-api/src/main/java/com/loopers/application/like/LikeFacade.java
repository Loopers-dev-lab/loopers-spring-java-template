package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeDomainService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class LikeFacade {
    private static final int retryCount = 3;

    private final LikeTxService likeTxService;
    private final LikeService likeService;
    private final ProductService productService;

    public void addLike(Long userId, Long productId) {
        long jitter = 0L;

        for (int i = 0; i < retryCount; i++) {
            try {
                likeTxService.addTxLike(userId, productId);
                return; // 성공 시 종료
            } catch (OptimisticLockingFailureException e) {
                try {
                    jitter = ThreadLocalRandom.current().nextLong(0, 100);
                    Thread.sleep(5 * (1 << i) + jitter);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                if (i == retryCount - 1) throw e;
            }
        }
    }

    public void removeLike(Long userId, Long productId) {
        long jitter = 0L;

        for (int i = 0; i < retryCount; i++) {
            try {
                likeTxService.removeTxLike(userId, productId);
                return;
            } catch (OptimisticLockingFailureException e) {
                try {
                    jitter = ThreadLocalRandom.current().nextLong(0, 100);
                    Thread.sleep(5 * (1 << i) + ThreadLocalRandom.current().nextLong(0, 100));
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                if (i == retryCount - 1) throw e;
            }
        }
    }

    public List<Product> getLikedProducts(Long userId) {
        List<Like> likes = likeService.findActiveLikesByUserId(userId);

        List<Long> productIds = likes.stream()
                .map(Like::getProductId)
                .toList();

        if (productIds.isEmpty()) {
            return List.of();
        }

        return productService.findAll(productIds);
    }
}
