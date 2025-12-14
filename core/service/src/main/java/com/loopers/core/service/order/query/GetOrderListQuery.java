package com.loopers.core.service.order.query;

import lombok.Getter;

@Getter
public class GetOrderListQuery {

    private final String userIdentifier;

    private final String createdAtSort;

    private final int pageNo;

    private final int pageSize;

    public GetOrderListQuery(String userIdentifier, String createdAtSort, int pageNo, int pageSize) {
        this.userIdentifier = userIdentifier;
        this.createdAtSort = createdAtSort;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }
}
