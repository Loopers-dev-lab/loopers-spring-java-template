package com.loopers.application.like;

import com.loopers.domain.like.ProductLike;
import com.loopers.domain.like.ProductLikeDomainService;
import com.loopers.domain.like.ProductLikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDomainService;
import com.loopers.domain.product.ProductLikeInfo;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.like.ProductLikeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Transactional
public class ProductLikeFacade {

    private final ProductLikeDomainService productLikeDomainService;
    private final ProductDomainService productDomainService;
    private final UserService userService;

    public ProductLikeDto.LikeResponse likeProduct(String userId, Long productId) {
        // 1. 사용자 조회
        User user = userService.findUser(userId);

        // 2. 상품 조회
        Product product = productDomainService.getProduct(productId);

        // 3. 도메인 서비스에 위임
        ProductLikeInfo info = productLikeDomainService.likeProduct(user, product);

        // 4. DTO 변환
        return ProductLikeDto.LikeResponse.from(info.liked(), info.totalLikes());
    }

    public ProductLikeDto.LikeResponse unlikeProduct(String userId, Long productId) {
        // 1. 사용자 조회
        User user = userService.findUser(userId);

        // 2. 상품 조회
        Product product = productDomainService.getProduct(productId);

        // 3. 도메인 서비스에 위임
        ProductLikeInfo info = productLikeDomainService.unlikeProduct(user, product);

        // 4. DTO 변환
        return ProductLikeDto.LikeResponse.from(info.liked(), info.totalLikes());
    }

    @Transactional(readOnly = true)
    public ProductLikeDto.LikedProductsResponse getLikedProducts(String userId) {
        // 1. 사용자 검증
        User user = userService.findUser(userId);

        // 2. 좋아요한 목록 조회
        List<Product> products = productLikeDomainService.getLikedProducts(user.getId());

        return ProductLikeDto.LikedProductsResponse.from(products);
    }
}
