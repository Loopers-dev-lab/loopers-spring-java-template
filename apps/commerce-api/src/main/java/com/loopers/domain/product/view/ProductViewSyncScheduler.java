package com.loopers.domain.product.view;

import com.loopers.domain.brand.QBrand;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.QProduct;
import com.loopers.infrastructure.product.ProductViewJpaRepository;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ProductView 테이블 동기화 스케줄러
 * 
 * 하이브리드 방식:
 * - 평소: 이벤트 기반 실시간 동기화 (ProductEvents.Created, ProductEvents.Deleted, ProductEvents.LikeCount)
 * - 주기적: 전체 재생성으로 정합성 보장 및 누락된 동기화 보정
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ProductViewSyncScheduler {

    private final JPAQueryFactory queryFactory;
    private final ProductViewJpaRepository productViewJpaRepository;
    private final LikeRepository likeRepository;

    private final QProduct product = QProduct.product;
    private final QBrand brand = QBrand.brand;
    private final QProductView productView = QProductView.productView;

    /**
     * 매일 새벽 3시에 ProductView 전체 재생성
     * 데이터 정합성 보장 + 누락된 동기화 보정
     */
    @Scheduled(cron = "0 0 3 * * *") // 매일 03:00
    @Transactional
    public void fullRefreshProductView() {
        log.info("ProductView 전체 재생성 시작");

        try {
            // 1. 기존 데이터 전체 삭제 (QueryDSL)
            long deletedCount = queryFactory
                    .delete(productView)
                    .execute();

            log.info("ProductView 전체 삭제 완료: {} rows", deletedCount);

            // 2. Product + Brand JOIN하여 전체 재생성
            List<Tuple> results = queryFactory
                    .select(
                            product.id,
                            product.name,
                            product.price,
                            product.brandId,
                            brand.name,  // brandName
                            product.status
                    )
                    .from(product)
                    .leftJoin(brand).on(product.brandId.eq(brand.id)
                            .and(brand.deletedAt.isNull()))
                    .where(product.deletedAt.isNull())  // Soft Delete 필터링
                    .fetch();

            // 3. ProductView로 변환 (LikeCount는 개별 조회)
            List<ProductView> views = results.stream()
                    .map(tuple -> {
                        Long productId = tuple.get(product.id);
                        
                        // LikeCount 조회
                        long likeCount = likeRepository.countByProductId(productId);
                        
                        return ProductView.builder()
                                .id(productId)
                                .name(tuple.get(product.name))
                                .price(tuple.get(product.price))
                                .likeCount(likeCount)
                                .brandId(tuple.get(product.brandId))
                                .brandName(tuple.get(brand.name))
                                .status(tuple.get(product.status))
                                .build();
                    })
                    .toList();

            // 4. 배치 저장
            productViewJpaRepository.saveAll(views);

            log.info("ProductView 전체 재생성 완료: {} rows", views.size());
        } catch (Exception e) {
            log.error("ProductView 전체 재생성 실패", e);
            throw e;  // 트랜잭션 롤백을 위해 예외 재발생
        }
    }
}

