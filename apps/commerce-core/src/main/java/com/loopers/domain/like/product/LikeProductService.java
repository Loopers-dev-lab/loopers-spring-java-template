package com.loopers.domain.like.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.event.EventType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class LikeProductService {
    private final LikeProductRepository likeProductRepository;

    @Transactional
    public LikeResult likeProduct(Long userId, Long productId) {
        Optional<LikeProduct> likeProduct = likeProductRepository.findByUserIdAndProductId(userId, productId);
        boolean beforeLiked = likeProduct.isPresent() && likeProduct.get().getDeletedAt() == null;
        if (beforeLiked) {
            // 멱등한 경우 이벤트 발행하지 않음
            return new LikeResult(true, true);
        }
        if (likeProduct.isPresent()) {
            likeProduct.get().restore();
            return new LikeResult(true, false);
        }
        LikeProduct newLikeProduct = LikeProduct.create(userId, productId);
        likeProductRepository.save(newLikeProduct);
        return new LikeResult(true, false);
    }

    @Transactional
    public LikeResult unlikeProduct(Long userId, Long productId) {
        Optional<LikeProduct> likeProduct = likeProductRepository.findByUserIdAndProductId(userId, productId);
        boolean beforeLiked = likeProduct.isPresent() && likeProduct.get().getDeletedAt() == null;
        likeProduct.ifPresent(BaseEntity::delete);
        return new LikeResult(false, beforeLiked);
    }

    public Page<LikeProduct> getLikedProducts(Long userId, Pageable pageable) {
        return likeProductRepository.getLikeProductsByUserIdAndDeletedAtIsNull(userId, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
    }
}
