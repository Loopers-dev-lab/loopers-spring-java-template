package com.loopers.application.like;

import com.loopers.domain.like.ProductLikeDomainService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductLikeInfo;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserDomainService;
import com.loopers.infrastructure.cache.ProductCacheService;
import com.loopers.interfaces.api.like.ProductLikeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLikeFacade {

    private final ProductLikeDomainService productLikeDomainService;
    private final UserDomainService userDomainService;
    private final ProductCacheService productCacheService;

    public ProductLikeDto.LikeResponse likeProduct(String userId, Long productId) {
        User user = userDomainService.findUser(userId);

        ProductLikeInfo info = productLikeDomainService.likeProduct(user, productId);
        invalidateCaches(productId);

        return ProductLikeDto.LikeResponse.from(info.liked(), info.totalLikes());
    }

    public ProductLikeDto.LikeResponse unlikeProduct(String userId, Long productId) {
        User user = userDomainService.findUser(userId);

        ProductLikeInfo info = productLikeDomainService.unlikeProduct(user, productId);
        invalidateCaches(productId);

        return ProductLikeDto.LikeResponse.from(info.liked(), info.totalLikes());
    }

    public ProductLikeDto.LikedProductsResponse getLikedProducts(String userId) {
        User user = userDomainService.findUser(userId);
        List<Product> products = productLikeDomainService.getLikedProducts(user.getId());
        return ProductLikeDto.LikedProductsResponse.from(products);
    }

    /**
     * 좋아요 상태 변경 시 캐시를 무효화합니다.
     *
     * 무효화 전략:
     * 1. 상세 캐시: 해당 상품의 detail 캐시 삭제
     * 2. 목록 캐시: 전체 리스트 캐시 무효화 (totalLikes 필드 반영)
     *
     * 트랜잭션 내에서 실행되며, 이후 새로운 요청이 오면
     * 캐시 미스 시 DB에서 최신 데이터를 읽어 캐시에 저장합니다.
     */
    private void invalidateCaches(Long productId) {
        productCacheService.deleteProductDetail(productId);
        productCacheService.invalidateProductListCaches();
    }
}
