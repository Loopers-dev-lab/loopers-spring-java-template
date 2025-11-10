package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.ProductModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "brand")
public class BrandModel extends BaseEntity {
    @Column(nullable = false,  length = 50)
    private String name;

    private String description;

    private Character status;

    /**
     * 브랜드 엔티티 입장에서, 상품목록
     * mappedBy="brand" 외리캐는 누가? Product 에서 소유. 거울
     * 주인은 Product의 brand
     * Brand의 products는 읽기/탐색용, 주인X 거울
     */
    @OneToMany(mappedBy = "brand", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<ProductModel> products = new ArrayList<>();
}
