package com.loopers.core.infra.database.mysql.order.repository;

import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.infra.database.mysql.order.dto.OrderListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderQuerydslRepository {

    Page<OrderListProjection> findListByCondition(Long userId, OrderSort createdAtSort, Pageable pageable);

}
