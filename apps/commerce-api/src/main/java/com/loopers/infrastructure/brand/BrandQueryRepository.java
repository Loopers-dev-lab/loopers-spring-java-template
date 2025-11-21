package com.loopers.infrastructure.brand;

import static com.loopers.domain.brand.QBrandEntity.brandEntity;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.dto.BrandSearchFilter;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@Component
@RequiredArgsConstructor
public class BrandQueryRepository {
    private final JPAQueryFactory queryFactory;

    public Page<BrandEntity> searchBrands(BrandSearchFilter filter, Pageable pageable) {
        List<BrandEntity> content = queryFactory
                .selectFrom(brandEntity)
                .where(
                        brandNameContains(filter.getBrandName()),
                        brandEntity.deletedAt.isNull()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(brandEntity.count())
                .from(brandEntity)
                .where(
                        brandNameContains(filter.getBrandName()),
                        brandEntity.deletedAt.isNull()
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    private BooleanExpression brandNameContains(String name) {
        return StringUtils.hasText(name) ? brandEntity.name.contains(name) : null;
    }
}
