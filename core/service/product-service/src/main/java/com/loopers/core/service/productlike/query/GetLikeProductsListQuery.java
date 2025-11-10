package com.loopers.core.service.productlike.query;

import lombok.Getter;

@Getter
public class GetLikeProductsListQuery {

    private final String userId;

    private final String brandId;

    private final String createdAtSort;

    private final String priceSort;

    private final String likeCountSort;

    private final int PageNo;

    private final int PageSize;

    public GetLikeProductsListQuery(
            String userId,
            String brandId,
            String createdAtSort,
            String priceSort,
            String likeCountSort,
            int pageNo,
            int pageSize
    ) {
        this.userId = userId;
        this.brandId = brandId;
        this.createdAtSort = createdAtSort;
        this.priceSort = priceSort;
        this.likeCountSort = likeCountSort;
        PageNo = pageNo;
        PageSize = pageSize;
    }
}
