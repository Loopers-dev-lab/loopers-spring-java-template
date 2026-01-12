package com.loopers.infrastructure.like;

import com.loopers.domain.like.LikeModel;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.product.ProductModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class LikeRepositoryImpl implements LikeRepository {

    private final LikeJpaRepository likeJpaRepository;

    // 좋아요 여부 조회
    @Override
    public Optional<LikeModel> findByUserAndProduct(UserModel user, ProductModel product) {
        return likeJpaRepository.findByUserAndProduct(user, product);
    }

    // 사용자가 좋아요한 상품 목록 조회
    @Override
    public List<ProductModel> findLikedProductsByUser(UserModel user) {
        return likeJpaRepository.findByUser(user).stream()
                .map(LikeModel::getProduct)
                .collect(Collectors.toList());
    }

    // 상품의 좋아요 수 조회
    @Override
    public long countByProductLiked(ProductModel product) {
        return likeJpaRepository.countByProduct(product);
    }

    // 상품의 좋아요 수 일괄 집계
    @Override
    public Map<Long, Long> countByProductIdsLiked(Collection<Long> productIds) {
        return likeJpaRepository.countByProductIds(productIds.stream().collect(Collectors.toSet()));
    }

    // 좋아요 등록
    @Override
    public LikeModel save(LikeModel like) {
        return likeJpaRepository.save(like);
    }

    // 좋아요 삭제
    @Override
    public void delete(LikeModel like) {
        likeJpaRepository.delete(like);
    }
}