package com.loopers.core.service.productlike;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.common.vo.PageNo;
import com.loopers.core.domain.common.vo.PageSize;
import com.loopers.core.domain.productlike.LikeProductListView;
import com.loopers.core.domain.productlike.repository.ProductLikeRepository;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.service.productlike.query.GetLikeProductsListQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductLikeQueryService {

    private final ProductLikeRepository repository;

    public LikeProductListView getLikeProductsListView(GetLikeProductsListQuery query) {
        return repository.findLikeProductsListWithCondition(
                new UserId(query.getUserId()),
                new BrandId(query.getBrandId()),
                OrderSort.from(query.getCreatedAtSort()),
                OrderSort.from(query.getPriceSort()),
                OrderSort.from(query.getLikeCountSort()),
                new PageNo(query.getPageNo()),
                new PageSize(query.getPageSize())
        );
    }
}
