package com.loopers.domain.like;

import com.loopers.domain.like.event.ProductLikedEvent;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductLikeInfo;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductLikeDomainService {

    private final ProductLikeRepository productLikeRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ProductLikeInfo likeProduct(User user, Long productId) {
        Product product = productRepository.findByIdOrThrow(productId);
        long currentLikeCount = product.getTotalLikes();

        Optional<ProductLike> existingLike = productLikeRepository
                .findByUserIdAndProductId(user.getId(), productId);

        if (existingLike.isPresent()) {
            return ProductLikeInfo.from(true, currentLikeCount);
        }

        ProductLike like = ProductLike.create(user.getId(), productId);
        productLikeRepository.save(like);

        eventPublisher.publishEvent(ProductLikedEvent.liked(productId, user.getId()));

        return ProductLikeInfo.from(true, currentLikeCount + 1);
    }

    @Transactional
    public ProductLikeInfo unlikeProduct(User user, Long productId) {
        Product product = productRepository.findByIdOrThrow(productId);
        long currentLikeCount = product.getTotalLikes();

        Optional<ProductLike> existingLike = productLikeRepository
                .findByUserIdAndProductId(user.getId(), productId);

        if (existingLike.isEmpty()) {
            return ProductLikeInfo.from(false, currentLikeCount);
        }

        productLikeRepository.delete(existingLike.get());

        eventPublisher.publishEvent(ProductLikedEvent.unliked(productId, user.getId()));

        return ProductLikeInfo.from(false, currentLikeCount - 1);
    }

    @Transactional(readOnly = true)
    public List<Product> getLikedProducts(Long userId) {
        // 좋아요한 상품 목록 조회
        List<Long> productIds = productLikeRepository.findProductIdsByUserId(userId);

        if (productIds.isEmpty()) {
            return List.of();
        }

        // 상품 정보 일괄 조회
        return productRepository.findAllByIdIn(productIds);
    }
}
