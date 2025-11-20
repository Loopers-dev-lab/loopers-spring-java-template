package com.loopers.application.like;

import com.loopers.domain.like.ProductLikeDomainService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDomainService;
import com.loopers.domain.product.ProductLikeInfo;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserDomainService;
import com.loopers.interfaces.api.like.ProductLikeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLikeFacade {

    private final ProductLikeDomainService productLikeDomainService;
    private final ProductDomainService productDomainService;
    private final UserDomainService userDomainService;


    public ProductLikeDto.LikeResponse likeProduct(String userId, Long productId) {
        // 사용자 조회
        User user = userDomainService.findUser(userId);

        // 상품 조회
        Product product = productDomainService.getProduct(productId);

        // 좋아요
        ProductLikeInfo info = productLikeDomainService.likeProduct(user, product);

        return ProductLikeDto.LikeResponse.from(info.liked(), info.totalLikes());
    }

    public ProductLikeDto.LikeResponse unlikeProduct(String userId, Long productId) {
        // 사용자 조회
        User user = userDomainService.findUser(userId);

        // 상품 조회
        Product product = productDomainService.getProduct(productId);

        // 좋아요 취소
        ProductLikeInfo info = productLikeDomainService.unlikeProduct(user, product);

        return ProductLikeDto.LikeResponse.from(info.liked(), info.totalLikes());
    }

    public ProductLikeDto.LikedProductsResponse getLikedProducts(String userId) {
        // 사용자
        User user = userDomainService.findUser(userId);

        // 좋아요한 목록 조회
        List<Product> products = productLikeDomainService.getLikedProducts(user.getId());

        return ProductLikeDto.LikedProductsResponse.from(products);
    }
}
