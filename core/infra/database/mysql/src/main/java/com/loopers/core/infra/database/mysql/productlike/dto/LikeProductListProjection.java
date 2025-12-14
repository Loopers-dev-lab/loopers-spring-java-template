package com.loopers.core.infra.database.mysql.productlike.dto;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.product.vo.*;
import com.loopers.core.domain.productlike.LikeProductListItem;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class LikeProductListProjection {

    private final Long id;

    private final Long brandId;

    private final String name;

    private final BigDecimal price;

    private final Long stock;

    private final Long likeCount;

    private final LocalDateTime createdAt;

    private final LocalDateTime updatedAt;

    @QueryProjection
    public LikeProductListProjection(
            Long id,
            Long brandId,
            String name,
            BigDecimal price,
            Long stock,
            Long likeCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.brandId = brandId;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public LikeProductListItem to() {
        return new LikeProductListItem(
                new ProductId(id.toString()),
                new BrandId(brandId.toString()),
                new ProductName(name),
                new ProductPrice(price),
                new ProductStock(stock),
                new ProductLikeCount(likeCount),
                new CreatedAt(createdAt),
                new UpdatedAt(updatedAt)
        );
    }
}
