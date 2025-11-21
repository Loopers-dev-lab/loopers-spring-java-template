package com.loopers.domain.like;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.product.ProductModel;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LikeRepository {

    // 좋아요 여부 조회
    Optional<LikeModel> findByUserAndProduct(UserModel user, ProductModel product);

    // 사용자가 좋아요한 상품 목록 조회
    List<ProductModel> findLikedProductsByUser(UserModel user);

    // 상품의 좋아요 수 조회
    long countByProductLiked(ProductModel product);

    // 상품의 좋아요 수 일괄 집계
    Map<Long, Long> countByProductIdsLiked(Collection<Long> productIds);

    // 좋아요 등록
    LikeModel save(LikeModel like);

    // 좋아요 삭제
    void delete(LikeModel like);
}