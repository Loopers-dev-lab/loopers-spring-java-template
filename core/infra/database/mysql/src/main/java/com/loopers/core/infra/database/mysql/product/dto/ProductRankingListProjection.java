package com.loopers.core.infra.database.mysql.product.dto;

import com.loopers.core.domain.brand.vo.BrandName;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.product.vo.ProductLikeCount;
import com.loopers.core.domain.product.vo.ProductName;
import com.loopers.core.domain.product.vo.ProductPrice;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.math.BigDecimal;

import static com.loopers.core.domain.product.ProductRankingList.ProductRankingItem;

@Getter
public class ProductRankingListProjection {

    private final Long id;

    private final String brandName;

    private final String name;

    private final BigDecimal price;

    private final Long likeCount;

    @QueryProjection
    public ProductRankingListProjection(Long id, String brandName, String name, BigDecimal price, Long likeCount) {
        this.id = id;
        this.brandName = brandName;
        this.name = name;
        this.price = price;
        this.likeCount = likeCount;
    }

    public ProductRankingItem to() {
        return new ProductRankingItem(
                new ProductId(this.id.toString()),
                null,
                new BrandName(this.brandName),
                new ProductName(this.name),
                new ProductPrice(this.price),
                new ProductLikeCount(this.likeCount),
                null
        );
    }
}
