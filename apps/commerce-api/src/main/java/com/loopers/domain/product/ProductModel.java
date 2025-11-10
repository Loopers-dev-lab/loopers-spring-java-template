package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.BrandModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "product")
public class ProductModel extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String productName;

    @Column(nullable = false, length = 32)
    private String category;

    private Integer price;

    private Integer stock;

    private Character status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private BrandModel brand;
}
