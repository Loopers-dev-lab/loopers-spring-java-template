package com.loopers.core.infra.database.mysql.order.repository;

import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.infra.database.mysql.order.dto.OrderListProjection;
import com.loopers.core.infra.database.mysql.order.dto.QOrderListProjection;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.loopers.core.infra.database.mysql.order.entity.QOrderEntity.orderEntity;

@Component
@RequiredArgsConstructor
public class OrderQuerydslRepositoryImpl implements OrderQuerydslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<OrderListProjection> findListByCondition(Long userId, OrderSort createdAtSort, Pageable pageable) {
        List<OrderListProjection> content = queryFactory
                .select(new QOrderListProjection(
                        orderEntity.id,
                        orderEntity.userId,
                        orderEntity.createdAt,
                        orderEntity.updatedAt
                ))
                .where(
                        orderEqUserId(userId),
                        orderEntity.deletedAt.isNull()
                )
                .from(orderEntity)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(
                        orderByCreatedAt(createdAtSort)
                ).fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(orderEntity.count())
                .from(orderEntity)
                .where(
                        orderEqUserId(userId),
                        orderEntity.deletedAt.isNull()
                );


        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression orderEqUserId(Long userId) {
        return Optional.ofNullable(userId)
                .map(orderEntity.userId::eq)
                .orElse(null);
    }

    private OrderSpecifier<?> orderByCreatedAt(OrderSort createdAtSort) {
        if (createdAtSort == OrderSort.ASC) return orderEntity.createdAt.asc();
        if (createdAtSort == OrderSort.DESC) return orderEntity.createdAt.desc();

        return null;
    }
}
