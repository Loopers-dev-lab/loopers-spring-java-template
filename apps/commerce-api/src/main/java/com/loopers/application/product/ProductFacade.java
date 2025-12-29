package com.loopers.application.product;

import com.loopers.application.ranking.RankingFacade;
import com.loopers.domain.product.ProductSearchCondition;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserActionEvent;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductFacade {

    private final ProductCacheService productCacheService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final RankingFacade rankingFacade;

    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long productId, String loginId) {
        ProductDetailInfo productDetail = productCacheService.getProductDetailWithCache(productId);

        Long rank = rankingFacade.getProductRankToday(productId);
        productDetail = productDetail.withRank(rank);

        // 유저 행동 로깅
        if (loginId != null) {
            try {
                User user = userService.getUserByLoginId(loginId);
                eventPublisher.publishEvent(UserActionEvent.productView(user.getId(), productId));
                log.debug("상품 조회 이벤트 발행: userId={}, productId={}", user.getId(), productId);
            } catch (Exception e) {
                // 유저 조회 실패해도 상품 조회는 정상 진행
                log.warn("상품 조회 이벤트 발행 실패: userId={}, productId={}", loginId, productId);
            }
        }

        return productDetail;
    }

    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long productId) {
        ProductDetailInfo productDetail = productCacheService.getProductDetailWithCache(productId);

        // 랭킹 정보 추가
        Long rank = rankingFacade.getProductRankToday(productId);
        return productDetail.withRank(rank);
    }

    public ProductListInfo getProducts(ProductGetListCommand command) {
        String cacheKey = String.format("brand:%s:sort:%s:page:%d:size:%d",
                command.brandId() != null ? command.brandId() : "all",
                command.getSortType().name().toLowerCase(),
                command.pageable().getPageNumber(),
                command.pageable().getPageSize()
        );

        ProductSearchCondition condition = new ProductSearchCondition(
                command.brandId(),
                command.getSortType(),
                command.pageable()
        );

        return productCacheService.getProductListWithCache(cacheKey, condition);
    }
}
