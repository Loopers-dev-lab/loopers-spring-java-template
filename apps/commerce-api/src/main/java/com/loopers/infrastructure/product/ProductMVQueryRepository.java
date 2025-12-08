package com.loopers.infrastructure.product;

import static com.loopers.domain.product.QProductMaterializedViewEntity.productMaterializedViewEntity;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.loopers.domain.brand.QBrandEntity;
import com.loopers.domain.like.QLikeEntity;
import com.loopers.domain.product.ProductMVSyncDto;
import com.loopers.domain.product.ProductMaterializedViewEntity;
import com.loopers.domain.product.QProductEntity;
import com.loopers.domain.product.dto.ProductSearchFilter;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * 상품 Materialized View QueryDSL 리포지토리
 * <p>
 * QueryDSL을 활용한 동적 쿼리 및 복잡한 조회 로직을 처리합니다.
 * 페이징, 정렬, 필터링 등의 기능을 제공합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 27.
 */
@Component
@RequiredArgsConstructor
public class ProductMVQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 브랜드별 상품 MV를 페이징 조회합니다.
     *
     * @param brandId  브랜드 ID
     * @param pageable 페이징 정보
     * @return 페이징된 상품 MV 목록
     */
    public Page<ProductMaterializedViewEntity> findByBrandId(Long brandId, Pageable pageable) {
        BooleanExpression whereCondition = brandIdEq(brandId);
        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(pageable);

        List<ProductMaterializedViewEntity> content = queryFactory
                .selectFrom(productMaterializedViewEntity)
                .where(whereCondition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .fetch();

        Long total = queryFactory
                .select(productMaterializedViewEntity.count())
                .from(productMaterializedViewEntity)
                .where(whereCondition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 상품명으로 검색하여 MV를 페이징 조회합니다.
     *
     * @param keyword  검색 키워드
     * @param pageable 페이징 정보
     * @return 페이징된 상품 MV 목록
     */
    public Page<ProductMaterializedViewEntity> findByNameContaining(String keyword, Pageable pageable) {
        BooleanExpression whereCondition = nameContains(keyword);
        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(pageable);

        List<ProductMaterializedViewEntity> content = queryFactory
                .selectFrom(productMaterializedViewEntity)
                .where(whereCondition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .fetch();

        Long total = queryFactory
                .select(productMaterializedViewEntity.count())
                .from(productMaterializedViewEntity)
                .where(whereCondition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 검색 필터를 기반으로 상품 MV를 페이징 조회합니다.
     *
     * @param searchFilter 검색 필터 (브랜드 ID, 상품명, 페이징 정보)
     * @return 페이징된 상품 MV 목록
     */
    public Page<ProductMaterializedViewEntity> findBySearchFilter(ProductSearchFilter searchFilter) {
        BooleanExpression whereCondition = buildWhereCondition(searchFilter);
        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(searchFilter.pageable());

        List<ProductMaterializedViewEntity> content = queryFactory
                .selectFrom(productMaterializedViewEntity)
                .where(whereCondition)
                .offset(searchFilter.pageable().getOffset())
                .limit(searchFilter.pageable().getPageSize())
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .fetch();

        Long total = queryFactory
                .select(productMaterializedViewEntity.count())
                .from(productMaterializedViewEntity)
                .where(whereCondition)
                .fetchOne();

        return new PageImpl<>(content, searchFilter.pageable(), total != null ? total : 0);
    }

    /**
     * 검색 필터 기반 where 조건을 빌드합니다.
     *
     * @param searchFilter 검색 필터
     * @return where 조건식
     */
    private BooleanExpression buildWhereCondition(ProductSearchFilter searchFilter) {
        BooleanExpression whereCondition = null;

        // 브랜드 ID 조건
        if (searchFilter.brandId() != null) {
            whereCondition = brandIdEq(searchFilter.brandId());
        }

        // 상품명 검색 조건
        if (searchFilter.productName() != null && !searchFilter.productName().trim().isEmpty()) {
            BooleanExpression nameCondition = nameContains(searchFilter.productName());
            whereCondition = whereCondition != null ? whereCondition.and(nameCondition) : nameCondition;
        }

        return whereCondition;
    }

    /**
     * Pageable 기반 정렬 조건을 빌드합니다.
     *
     * @param pageable 페이징 정보
     * @return 정렬 조건 목록
     */
    private List<OrderSpecifier<?>> buildOrderSpecifiers(Pageable pageable) {
        if (pageable.getSort().isEmpty()) {
            // 기본 정렬: 좋아요 수 내림차순
            return List.of(productMaterializedViewEntity.likeCount.desc());
        }

        return pageable.getSort().stream()
                .map(sort -> {
                    String property = sort.getProperty();
                    boolean isAsc = sort.isAscending();
                    return switch (property) {
                        case "likeCount" -> isAsc ?
                                productMaterializedViewEntity.likeCount.asc() :
                                productMaterializedViewEntity.likeCount.desc();
                        case "name" -> isAsc ?
                                productMaterializedViewEntity.name.asc() :
                                productMaterializedViewEntity.name.desc();
                        case "price" -> isAsc ?
                                productMaterializedViewEntity.price.originPrice.asc() :
                                productMaterializedViewEntity.price.originPrice.desc();
                        case "createdAt" -> isAsc ?
                                productMaterializedViewEntity.createdAt.asc() :
                                productMaterializedViewEntity.createdAt.desc();
                        case "updatedAt" -> isAsc ?
                                productMaterializedViewEntity.updatedAt.asc() :
                                productMaterializedViewEntity.updatedAt.desc();
                        default -> productMaterializedViewEntity.likeCount.desc();
                    };
                })
                .collect(Collectors.toList());
    }

    /**
     * 브랜드 ID 동등 조건을 생성합니다.
     *
     * @param brandId 브랜드 ID
     * @return 조건식
     */
    private BooleanExpression brandIdEq(Long brandId) {
        return brandId != null ? productMaterializedViewEntity.brandId.eq(brandId) : null;
    }

    /**
     * 상품명 포함 조건을 생성합니다.
     *
     * @param keyword 검색 키워드
     * @return 조건식
     */
    private BooleanExpression nameContains(String keyword) {
        return keyword != null && !keyword.isBlank() ?
                productMaterializedViewEntity.name.contains(keyword) : null;
    }


    /**
     * 지정된 시간 이후 변경된 상품 MV를 조회합니다.
     * <p>
     * 상품, 좋아요, 브랜드 중 하나라도 변경된 경우를 감지합니다.
     * 여유 시간(기본 1분)을 추가하여 타이밍 이슈를 방지합니다.
     *
     * @param syncedTime    마지막 배치 실행 시간
     * @param marginMinutes 여유 시간 (분)
     * @return 변경된 MV 엔티티 목록
     */
    public List<ProductMaterializedViewEntity> findChangedSince(
            ZonedDateTime syncedTime,
            int marginMinutes
    ) {
        // 여유 시간을 적용한 기준 시간 설정
        ZonedDateTime baseTime = syncedTime.minusMinutes(marginMinutes);


        return queryFactory
                .selectFrom(productMaterializedViewEntity)
                .where(
                        // 상품, 좋아요, 브랜드 중 하나라도 변경된 경우
                        productMaterializedViewEntity.productUpdatedAt.after(baseTime)
                                .or(productMaterializedViewEntity.likeUpdatedAt.after(baseTime))
                                .or(productMaterializedViewEntity.brandUpdatedAt.after(baseTime)),
                        // 삭제되지 않은 MV만 조회
                        productMaterializedViewEntity.deletedAt.isNull()
                )
                .orderBy(productMaterializedViewEntity.productUpdatedAt.desc())
                .fetch();
    }

    /**
     * 마지막 배치 시간 이후 변경된 데이터를 Product, Brand, Like 조인으로 조회합니다.
     * <p>
     * 단일 쿼리로 변경된 상품만 조회하여 성능을 최적화합니다.
     *
     * @param lastBatchTime 마지막 배치 실행 시간
     * @return 변경된 상품 데이터 DTO 목록
     */
    public List<ProductMVSyncDto> findChangedProductsForSync(ZonedDateTime lastBatchTime) {
        QProductEntity product = QProductEntity.productEntity;
        QBrandEntity brand = QBrandEntity.brandEntity;
        QLikeEntity like = QLikeEntity.likeEntity;

        return queryFactory
                .select(Projections.constructor(
                        ProductMVSyncDto.class,
                        // 상품 정보
                        product.id,
                        product.name,
                        product.description,
                        product.price.originPrice,
                        product.price.discountPrice,
                        product.stockQuantity,
                        product.updatedAt,
                        // 브랜드 정보
                        brand.id,
                        brand.name,
                        brand.updatedAt,
                        // 좋아요 정보
                        like.id.count().coalesce(0L),
                        like.updatedAt.max()
                ))
                .from(product)
                .leftJoin(brand)
                .on(product.brandId.eq(brand.id))
                .leftJoin(like)
                .on(like.productId.eq(product.id)
                        .and(like.deletedAt.isNull()))
                .where(
                        // 상품, 브랜드, 좋아요 중 하나라도 변경된 경우
                        product.updatedAt.after(lastBatchTime)
                                .or(brand.updatedAt.after(lastBatchTime))
                                .or(like.updatedAt.after(lastBatchTime)),
                        // 삭제되지 않은 상품만
                        product.deletedAt.isNull(),
                        brand.deletedAt.isNull()
                )
                .groupBy(
                        product.id,
                        product.name,
                        product.description,
                        product.price.originPrice,
                        product.price.discountPrice,
                        product.stockQuantity,
                        product.updatedAt,
                        brand.id,
                        brand.name,
                        brand.updatedAt
                )
                .fetch();
    }

}
